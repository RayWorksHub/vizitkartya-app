-- Atomic, idempotent profile snapshot sync for offline-first mobile clients.

alter table public.profiles
    add column if not exists sync_version bigint not null default 0;

-- Profiles that predate versioned sync start at 1 so an unbased offline client cannot overwrite them.
update public.profiles set sync_version = 1 where sync_version = 0;

alter table public.profiles
    add constraint profiles_sync_version_nonnegative check (sync_version >= 0);

create table public.profile_sync_operations (
    operation_id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    resulting_version bigint not null,
    response jsonb not null,
    created_at timestamptz not null default now(),
    constraint profile_sync_operations_response_object
        check (jsonb_typeof(response) = 'object')
);

create index profile_sync_operations_user_created_idx
    on public.profile_sync_operations(user_id, created_at desc);

alter table public.profile_sync_operations enable row level security;
revoke all on public.profile_sync_operations from anon, authenticated;

create or replace function public.profile_snapshot_for_user(p_user_id uuid)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
    select jsonb_build_object(
        'firstName', p.first_name,
        'lastName', p.last_name,
        'displayName', p.display_name,
        'company', p.company,
        'jobTitle', p.job_title,
        'bio', p.bio,
        'displayImagePath', p.display_image_path,
        'contactImagePath', p.contact_image_path,
        'logoPath', p.logo_path,
        'publicSlug', p.public_slug,
        'isPublic', coalesce(s.is_public, false),
        'fieldOrder', coalesce(s.field_order, '[]'::jsonb),
        'fieldVisibility', coalesce(s.field_visibility, '{}'::jsonb),
        'contacts', coalesce((
            select jsonb_agg(jsonb_build_object(
                'id', c.id::text,
                'kind', c.kind,
                'label', c.label,
                'value', c.value,
                'sortOrder', c.sort_order,
                'isPublic', c.is_public
            ) order by c.sort_order, c.id)
            from public.profile_contacts c
            where c.user_id = p.user_id
        ), '[]'::jsonb),
        'addresses', coalesce((
            select jsonb_agg(jsonb_build_object(
                'id', a.id::text,
                'label', a.label,
                'formattedAddress', a.formatted_address,
                'sortOrder', a.sort_order,
                'isPublic', a.is_public
            ) order by a.sort_order, a.id)
            from public.profile_addresses a
            where a.user_id = p.user_id
        ), '[]'::jsonb),
        'links', coalesce((
            select jsonb_agg(jsonb_build_object(
                'id', l.id::text,
                'kind', l.kind,
                'label', l.label,
                'url', l.url,
                'sortOrder', l.sort_order,
                'isPublic', l.is_public
            ) order by l.sort_order, l.id)
            from public.profile_links l
            where l.user_id = p.user_id
        ), '[]'::jsonb)
    )
    from public.profiles p
    left join public.profile_settings s on s.user_id = p.user_id
    where p.user_id = p_user_id
    limit 1;
$$;

revoke all on function public.profile_snapshot_for_user(uuid) from public;

create or replace function public.get_my_profile_snapshot()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_user_id uuid := auth.uid();
    v_version bigint;
    v_snapshot jsonb;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '42501';
    end if;

    select p.sync_version
    into v_version
    from public.profiles p
    where p.user_id = v_user_id;

    if not found then
        return jsonb_build_object(
            'status', 'current',
            'serverVersion', 0,
            'snapshot', null
        );
    end if;

    v_snapshot := public.profile_snapshot_for_user(v_user_id);
    return jsonb_build_object(
        'status', 'current',
        'serverVersion', v_version,
        'snapshot', v_snapshot
    );
end;
$$;

revoke all on function public.get_my_profile_snapshot() from public;
grant execute on function public.get_my_profile_snapshot() to authenticated;

create or replace function public.sync_profile_snapshot(
    p_operation_id uuid,
    p_base_version bigint,
    p_snapshot jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid := auth.uid();
    v_current_version bigint;
    v_new_version bigint;
    v_response jsonb;
    v_contacts jsonb := coalesce(p_snapshot -> 'contacts', '[]'::jsonb);
    v_addresses jsonb := coalesce(p_snapshot -> 'addresses', '[]'::jsonb);
    v_links jsonb := coalesce(p_snapshot -> 'links', '[]'::jsonb);
    v_field_order jsonb := coalesce(p_snapshot -> 'fieldOrder', '[]'::jsonb);
    v_field_visibility jsonb := coalesce(p_snapshot -> 'fieldVisibility', '{}'::jsonb);
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '42501';
    end if;
    if p_operation_id is null or p_base_version < 0 then
        raise exception 'Invalid sync metadata' using errcode = '22023';
    end if;

    delete from public.profile_sync_operations
    where user_id = v_user_id
      and created_at < now() - interval '90 days';

    if jsonb_typeof(p_snapshot) is distinct from 'object'
        or jsonb_typeof(v_contacts) is distinct from 'array'
        or jsonb_typeof(v_addresses) is distinct from 'array'
        or jsonb_typeof(v_links) is distinct from 'array'
        or jsonb_typeof(v_field_order) is distinct from 'array'
        or jsonb_typeof(v_field_visibility) is distinct from 'object'
    then
        raise exception 'Invalid profile snapshot shape' using errcode = '22023';
    end if;
    if jsonb_array_length(v_contacts) > 20
        or jsonb_array_length(v_addresses) > 10
        or jsonb_array_length(v_links) > 30
    then
        raise exception 'Profile snapshot exceeds item limits' using errcode = '22023';
    end if;
    if octet_length(p_snapshot::text) > 262144 then
        raise exception 'Profile snapshot is too large' using errcode = '22001';
    end if;

    select o.response
    into v_response
    from public.profile_sync_operations o
    where o.operation_id = p_operation_id
      and o.user_id = v_user_id;
    if found then
        return v_response;
    end if;

    insert into public.profiles(user_id)
    values (v_user_id)
    on conflict (user_id) do nothing;
    insert into public.profile_settings(user_id)
    values (v_user_id)
    on conflict (user_id) do nothing;

    select p.sync_version
    into v_current_version
    from public.profiles p
    where p.user_id = v_user_id
    for update;

    -- A concurrent retry with the same id may have completed while this call waited for the row lock.
    select o.response
    into v_response
    from public.profile_sync_operations o
    where o.operation_id = p_operation_id
      and o.user_id = v_user_id;
    if found then
        return v_response;
    end if;

    if v_current_version <> p_base_version then
        v_response := jsonb_build_object(
            'status', 'conflict',
            'serverVersion', v_current_version,
            'snapshot', public.profile_snapshot_for_user(v_user_id)
        );
        insert into public.profile_sync_operations(
            operation_id,
            user_id,
            resulting_version,
            response
        ) values (
            p_operation_id,
            v_user_id,
            v_current_version,
            v_response
        );
        return v_response;
    end if;

    update public.profiles
    set first_name = btrim(coalesce(p_snapshot ->> 'firstName', '')),
        last_name = btrim(coalesce(p_snapshot ->> 'lastName', '')),
        display_name = btrim(coalesce(p_snapshot ->> 'displayName', '')),
        company = btrim(coalesce(p_snapshot ->> 'company', '')),
        job_title = btrim(coalesce(p_snapshot ->> 'jobTitle', '')),
        bio = btrim(coalesce(p_snapshot ->> 'bio', '')),
        display_image_path = nullif(btrim(p_snapshot ->> 'displayImagePath'), ''),
        contact_image_path = nullif(btrim(p_snapshot ->> 'contactImagePath'), ''),
        logo_path = nullif(btrim(p_snapshot ->> 'logoPath'), ''),
        public_slug = nullif(lower(btrim(p_snapshot ->> 'publicSlug')), ''),
        sync_version = v_current_version + 1
    where user_id = v_user_id
    returning sync_version into v_new_version;

    delete from public.profile_contacts where user_id = v_user_id;
    insert into public.profile_contacts(
        id,
        user_id,
        kind,
        label,
        value,
        sort_order,
        is_public
    )
    select
        (item ->> 'id')::uuid,
        v_user_id,
        item ->> 'kind',
        btrim(coalesce(item ->> 'label', '')),
        btrim(item ->> 'value'),
        coalesce((item ->> 'sortOrder')::integer, 0),
        coalesce((item ->> 'isPublic')::boolean, false)
    from jsonb_array_elements(v_contacts) item
    where nullif(btrim(item ->> 'value'), '') is not null;

    delete from public.profile_addresses where user_id = v_user_id;
    insert into public.profile_addresses(
        id,
        user_id,
        label,
        formatted_address,
        sort_order,
        is_public
    )
    select
        (item ->> 'id')::uuid,
        v_user_id,
        btrim(coalesce(item ->> 'label', '')),
        btrim(item ->> 'formattedAddress'),
        coalesce((item ->> 'sortOrder')::integer, 0),
        coalesce((item ->> 'isPublic')::boolean, false)
    from jsonb_array_elements(v_addresses) item
    where nullif(btrim(item ->> 'formattedAddress'), '') is not null;

    delete from public.profile_links where user_id = v_user_id;
    insert into public.profile_links(
        id,
        user_id,
        kind,
        label,
        url,
        sort_order,
        is_public
    )
    select
        (item ->> 'id')::uuid,
        v_user_id,
        coalesce(nullif(btrim(item ->> 'kind'), ''), 'custom'),
        btrim(coalesce(item ->> 'label', '')),
        btrim(item ->> 'url'),
        coalesce((item ->> 'sortOrder')::integer, 0),
        coalesce((item ->> 'isPublic')::boolean, false)
    from jsonb_array_elements(v_links) item
    where nullif(btrim(item ->> 'url'), '') is not null;

    insert into public.profile_settings(
        user_id,
        is_public,
        field_order,
        field_visibility
    ) values (
        v_user_id,
        coalesce((p_snapshot ->> 'isPublic')::boolean, false),
        v_field_order,
        v_field_visibility
    )
    on conflict (user_id) do update
    set is_public = excluded.is_public,
        field_order = excluded.field_order,
        field_visibility = excluded.field_visibility;

    v_response := jsonb_build_object(
        'status', 'applied',
        'serverVersion', v_new_version,
        'snapshot', public.profile_snapshot_for_user(v_user_id)
    );

    insert into public.profile_sync_operations(
        operation_id,
        user_id,
        resulting_version,
        response
    ) values (
        p_operation_id,
        v_user_id,
        v_new_version,
        v_response
    );
    return v_response;
end;
$$;

revoke all on function public.sync_profile_snapshot(uuid, bigint, jsonb) from public;
grant execute on function public.sync_profile_snapshot(uuid, bigint, jsonb) to authenticated;

-- All aggregate writes now go through the version-checked RPC above. Owners keep read access.
revoke insert, update, delete on public.profiles from authenticated;
revoke insert, update, delete on public.profile_contacts from authenticated;
revoke insert, update, delete on public.profile_addresses from authenticated;
revoke insert, update, delete on public.profile_links from authenticated;
revoke insert, update, delete on public.profile_settings from authenticated;
