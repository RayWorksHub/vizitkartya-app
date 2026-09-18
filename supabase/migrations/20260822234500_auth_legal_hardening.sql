-- Legal-acceptance gate for verified email, OAuth and pre-existing VIZIT accounts.

alter table public.legal_acceptances
    add constraint legal_acceptances_privacy_version_length
        check (char_length(privacy_policy_version) <= 128) not valid,
    add constraint legal_acceptances_terms_version_length
        check (char_length(terms_version) <= 128) not valid;

alter table public.legal_acceptances
    validate constraint legal_acceptances_privacy_version_length;
alter table public.legal_acceptances
    validate constraint legal_acceptances_terms_version_length;

create or replace function public.has_legal_acceptance(
    p_privacy_policy_version text,
    p_terms_version text
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select auth.uid() is not null
       and nullif(btrim(p_privacy_policy_version), '') is not null
       and nullif(btrim(p_terms_version), '') is not null
       and exists (
            select 1
            from public.legal_acceptances acceptance
            where acceptance.user_id = auth.uid()
              and acceptance.privacy_policy_version = btrim(p_privacy_policy_version)
              and acceptance.terms_version = btrim(p_terms_version)
       );
$$;

revoke all on function public.has_legal_acceptance(text, text) from public;
grant execute on function public.has_legal_acceptance(text, text) to authenticated;

create or replace function public.has_any_legal_acceptance()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select auth.uid() is not null
       and exists (
            select 1
            from public.legal_acceptances acceptance
            where acceptance.user_id = auth.uid()
       );
$$;

revoke all on function public.has_any_legal_acceptance() from public;

create or replace function public.accept_legal_documents(
    p_privacy_policy_version text,
    p_terms_version text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid := auth.uid();
    v_privacy_version text := btrim(p_privacy_policy_version);
    v_terms_version text := btrim(p_terms_version);
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '42501';
    end if;
    if nullif(v_privacy_version, '') is null
        or nullif(v_terms_version, '') is null
        or char_length(v_privacy_version) > 128
        or char_length(v_terms_version) > 128
    then
        raise exception 'Invalid legal document version' using errcode = '22023';
    end if;

    insert into public.legal_acceptances(
        user_id,
        privacy_policy_version,
        terms_version
    ) values (
        v_user_id,
        v_privacy_version,
        v_terms_version
    )
    on conflict (user_id, privacy_policy_version, terms_version) do nothing;
end;
$$;

revoke all on function public.accept_legal_documents(text, text) from public;
grant execute on function public.accept_legal_documents(text, text) to authenticated;

-- Supabase access tokens remain valid until expiry after Auth-user deletion. Storage ownership
-- therefore also requires that the token subject still exists in auth.users.
create or replace function public.is_active_vizit_user()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select auth.uid() is not null
       and exists (
            select 1
            from auth.users user_account
            where user_account.id = auth.uid()
       );
$$;

revoke all on function public.is_active_vizit_user() from public;
grant execute on function public.is_active_vizit_user() to authenticated;

drop policy if exists "profile_private_owner_select" on storage.objects;
create policy "profile_private_owner_select" on storage.objects
for select to authenticated
using (
    bucket_id = 'profile-private'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "profile_private_owner_insert" on storage.objects;
create policy "profile_private_owner_insert" on storage.objects
for insert to authenticated
with check (
    bucket_id = 'profile-private'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "profile_private_owner_update" on storage.objects;
create policy "profile_private_owner_update" on storage.objects
for update to authenticated
using (
    bucket_id = 'profile-private'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'profile-private'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "profile_private_owner_delete" on storage.objects;
create policy "profile_private_owner_delete" on storage.objects
for delete to authenticated
using (
    bucket_id = 'profile-private'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "profile_public_owner_insert" on storage.objects;
create policy "profile_public_owner_insert" on storage.objects
for insert to authenticated
with check (
    bucket_id = 'profile-public'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "profile_public_owner_update" on storage.objects;
create policy "profile_public_owner_update" on storage.objects
for update to authenticated
using (
    bucket_id = 'profile-public'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'profile-public'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "profile_public_owner_delete" on storage.objects;
create policy "profile_public_owner_delete" on storage.objects
for delete to authenticated
using (
    bucket_id = 'profile-public'
    and public.is_active_vizit_user()
    and (storage.foldername(name))[1] = auth.uid()::text
);

-- Keep the original snapshot implementations private and place the legal gate in stable wrappers.
alter function public.get_my_profile_snapshot()
    rename to get_my_profile_snapshot_after_legal_acceptance;
revoke all on function public.get_my_profile_snapshot_after_legal_acceptance() from public;

create function public.get_my_profile_snapshot()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if not public.has_any_legal_acceptance() then
        raise exception 'Legal acceptance required' using errcode = '42501';
    end if;
    return public.get_my_profile_snapshot_after_legal_acceptance();
end;
$$;

revoke all on function public.get_my_profile_snapshot() from public;
grant execute on function public.get_my_profile_snapshot() to authenticated;

alter function public.sync_profile_snapshot(uuid, bigint, jsonb)
    rename to sync_profile_snapshot_after_legal_acceptance;
revoke all on function public.sync_profile_snapshot_after_legal_acceptance(uuid, bigint, jsonb) from public;

create function public.sync_profile_snapshot(
    p_operation_id uuid,
    p_base_version bigint,
    p_snapshot jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not public.has_any_legal_acceptance() then
        raise exception 'Legal acceptance required' using errcode = '42501';
    end if;
    return public.sync_profile_snapshot_after_legal_acceptance(
        p_operation_id,
        p_base_version,
        p_snapshot
    );
end;
$$;

revoke all on function public.sync_profile_snapshot(uuid, bigint, jsonb) from public;
grant execute on function public.sync_profile_snapshot(uuid, bigint, jsonb) to authenticated;
