begin;

create extension if not exists pgcrypto with schema extensions;

create table public.user_accounts (
    id uuid primary key references auth.users (id) on delete cascade,
    locale text not null default 'hu-HU',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.profiles (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null unique references auth.users (id) on delete cascade,
    first_name text not null default '',
    last_name text not null default '',
    display_name text not null default '',
    company_name text not null default '',
    job_title text not null default '',
    bio text not null default '',
    profile_image_path text,
    public_profile_image_path text,
    company_logo_path text,
    public_company_logo_path text,
    public_slug text,
    is_public boolean not null default false,
    published_at timestamptz,
    revision bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profiles_public_slug_format check (
        public_slug is null or public_slug ~ '^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$'
    ),
    constraint profiles_public_requires_slug check (not is_public or public_slug is not null),
    constraint profiles_published_at_consistency check (
        (is_public and published_at is not null) or (not is_public)
    )
);

create table public.profile_settings (
    profile_id uuid primary key references public.profiles (id) on delete cascade,
    theme text not null default 'system' check (theme in ('system', 'light', 'dark')),
    dynamic_color_enabled boolean not null default false,
    analytics_enabled boolean not null default true,
    publication_consent_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.profile_field_settings (
    profile_id uuid not null references public.profiles (id) on delete cascade,
    field_key text not null,
    is_public boolean not null default false,
    sort_order integer not null default 0 check (sort_order >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (profile_id, field_key),
    constraint profile_field_settings_key check (
        field_key in (
            'first_name',
            'last_name',
            'display_name',
            'company_name',
            'job_title',
            'bio',
            'profile_image',
            'company_logo'
        )
    )
);

create table public.profile_phone_numbers (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    label text not null default 'mobile',
    phone_number text not null,
    is_public boolean not null default false,
    sort_order integer not null default 0 check (sort_order >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profile_phone_numbers_not_blank check (btrim(phone_number) <> '')
);

create table public.profile_email_addresses (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    label text not null default 'work',
    email_address text not null,
    is_public boolean not null default false,
    sort_order integer not null default 0 check (sort_order >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profile_email_addresses_not_blank check (btrim(email_address) <> ''),
    constraint profile_email_addresses_basic_format check (
        email_address ~* '^[^[:space:]@]+@[^[:space:]@]+[.][^[:space:]@]+$'
    )
);

create table public.profile_addresses (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    label text not null default 'work',
    formatted_address text not null default '',
    address_line_1 text not null default '',
    address_line_2 text not null default '',
    city text not null default '',
    postal_code text not null default '',
    region text not null default '',
    country_code text not null default 'HU',
    is_public boolean not null default false,
    sort_order integer not null default 0 check (sort_order >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profile_addresses_country_code check (country_code ~ '^[A-Z]{2}$'),
    constraint profile_addresses_has_content check (
        btrim(formatted_address) <> '' or
        btrim(address_line_1) <> '' or
        btrim(city) <> ''
    )
);

create table public.profile_links (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    link_type text not null,
    label text not null default '',
    url text not null,
    is_public boolean not null default false,
    sort_order integer not null default 0 check (sort_order >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profile_links_type check (
        link_type in (
            'website',
            'linkedin',
            'instagram',
            'facebook',
            'tiktok',
            'youtube',
            'custom'
        )
    ),
    constraint profile_links_not_blank check (btrim(url) <> ''),
    constraint profile_links_https check (url ~* '^https://')
);

create table public.qr_settings (
    profile_id uuid primary key references public.profiles (id) on delete cascade,
    default_mode text not null default 'profile' check (default_mode in ('profile', 'contact')),
    profile_qr_enabled boolean not null default true,
    contact_qr_enabled boolean not null default true,
    error_correction text not null default 'M' check (error_correction in ('L', 'M', 'Q', 'H')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.nfc_settings (
    profile_id uuid primary key references public.profiles (id) on delete cascade,
    contact_share_enabled boolean not null default true,
    include_contact_photo boolean not null default true,
    profile_url_fallback_enabled boolean not null default true,
    payload_limit_bytes integer not null default 16384 check (
        payload_limit_bytes between 512 and 32767
    ),
    timeout_seconds integer not null default 60 check (timeout_seconds between 15 and 120),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.share_events (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles (id) on delete cascade,
    client_event_id uuid not null,
    channel text not null check (
        channel in ('nfc_contact', 'nfc_profile_url', 'qr_contact', 'qr_profile', 'profile_link')
    ),
    result text not null check (
        result in ('started', 'payload_read', 'cancelled', 'timed_out', 'fallback_shown', 'failed')
    ),
    receiver_platform text check (
        receiver_platform is null or receiver_platform in ('android', 'ios', 'unknown')
    ),
    payload_bytes integer check (payload_bytes is null or payload_bytes >= 0),
    photo_included boolean,
    occurred_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    unique (profile_id, client_event_id)
);

create table public.analytics_events (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid references public.profiles (id) on delete cascade,
    client_event_id uuid not null,
    event_name text not null check (
        event_name in (
            'profile_view',
            'nfc_share_started',
            'nfc_payload_read',
            'nfc_share_cancelled',
            'nfc_fallback_shown',
            'qr_opened',
            'qr_shared',
            'public_profile_opened',
            'contact_action',
            'ios_fallback_used'
        )
    ),
    source text not null default 'android' check (source in ('android', 'public_profile', 'edge')),
    properties jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    constraint analytics_events_properties_object check (jsonb_typeof(properties) = 'object'),
    constraint analytics_events_properties_size check (octet_length(properties::text) <= 4096),
    unique (client_event_id)
);

create index profiles_owner_id_idx on public.profiles (owner_id);
create unique index profiles_public_slug_lower_idx
    on public.profiles (lower(public_slug))
    where public_slug is not null;
create index profile_phone_numbers_profile_order_idx
    on public.profile_phone_numbers (profile_id, sort_order, id);
create index profile_email_addresses_profile_order_idx
    on public.profile_email_addresses (profile_id, sort_order, id);
create index profile_addresses_profile_order_idx
    on public.profile_addresses (profile_id, sort_order, id);
create index profile_links_profile_order_idx
    on public.profile_links (profile_id, sort_order, id);
create index share_events_profile_time_idx
    on public.share_events (profile_id, occurred_at desc);
create index analytics_events_profile_time_idx
    on public.analytics_events (profile_id, occurred_at desc);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create or replace function public.set_profile_revision()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    new.revision = old.revision + 1;
    if new.is_public and not old.is_public and new.published_at is null then
        new.published_at = now();
    end if;
    return new;
end;
$$;

create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.user_accounts (id)
    values (new.id)
    on conflict (id) do nothing;
    return new;
end;
$$;

create or replace function public.initialize_profile_defaults()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profile_settings (profile_id) values (new.id);
    insert into public.qr_settings (profile_id) values (new.id);
    insert into public.nfc_settings (profile_id) values (new.id);

    insert into public.profile_field_settings (profile_id, field_key, sort_order)
    values
        (new.id, 'profile_image', 0),
        (new.id, 'display_name', 10),
        (new.id, 'first_name', 20),
        (new.id, 'last_name', 30),
        (new.id, 'job_title', 40),
        (new.id, 'company_name', 50),
        (new.id, 'company_logo', 60),
        (new.id, 'bio', 70);

    return new;
end;
$$;

create trigger user_accounts_set_updated_at
before update on public.user_accounts
for each row execute function public.set_updated_at();

create trigger profiles_set_revision
before update on public.profiles
for each row execute function public.set_profile_revision();

create trigger profile_settings_set_updated_at
before update on public.profile_settings
for each row execute function public.set_updated_at();

create trigger profile_field_settings_set_updated_at
before update on public.profile_field_settings
for each row execute function public.set_updated_at();

create trigger profile_phone_numbers_set_updated_at
before update on public.profile_phone_numbers
for each row execute function public.set_updated_at();

create trigger profile_email_addresses_set_updated_at
before update on public.profile_email_addresses
for each row execute function public.set_updated_at();

create trigger profile_addresses_set_updated_at
before update on public.profile_addresses
for each row execute function public.set_updated_at();

create trigger profile_links_set_updated_at
before update on public.profile_links
for each row execute function public.set_updated_at();

create trigger qr_settings_set_updated_at
before update on public.qr_settings
for each row execute function public.set_updated_at();

create trigger nfc_settings_set_updated_at
before update on public.nfc_settings
for each row execute function public.set_updated_at();

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

create trigger on_profile_created
after insert on public.profiles
for each row execute function public.initialize_profile_defaults();

alter table public.user_accounts enable row level security;
alter table public.profiles enable row level security;
alter table public.profile_settings enable row level security;
alter table public.profile_field_settings enable row level security;
alter table public.profile_phone_numbers enable row level security;
alter table public.profile_email_addresses enable row level security;
alter table public.profile_addresses enable row level security;
alter table public.profile_links enable row level security;
alter table public.qr_settings enable row level security;
alter table public.nfc_settings enable row level security;
alter table public.share_events enable row level security;
alter table public.analytics_events enable row level security;

create policy user_accounts_owner_all
on public.user_accounts
for all
to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

create policy profiles_owner_all
on public.profiles
for all
to authenticated
using ((select auth.uid()) = owner_id)
with check ((select auth.uid()) = owner_id);

create policy profile_settings_owner_all
on public.profile_settings
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = profile_settings.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = profile_settings.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy profile_field_settings_owner_all
on public.profile_field_settings
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = profile_field_settings.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = profile_field_settings.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy profile_phone_numbers_owner_all
on public.profile_phone_numbers
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = profile_phone_numbers.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = profile_phone_numbers.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy profile_email_addresses_owner_all
on public.profile_email_addresses
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = profile_email_addresses.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = profile_email_addresses.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy profile_addresses_owner_all
on public.profile_addresses
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = profile_addresses.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = profile_addresses.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy profile_links_owner_all
on public.profile_links
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = profile_links.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = profile_links.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy qr_settings_owner_all
on public.qr_settings
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = qr_settings.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = qr_settings.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy nfc_settings_owner_all
on public.nfc_settings
for all
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = nfc_settings.profile_id and p.owner_id = (select auth.uid())
    )
)
with check (
    exists (
        select 1 from public.profiles p
        where p.id = nfc_settings.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy share_events_owner_select
on public.share_events
for select
to authenticated
using (
    exists (
        select 1 from public.profiles p
        where p.id = share_events.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy share_events_owner_insert
on public.share_events
for insert
to authenticated
with check (
    exists (
        select 1 from public.profiles p
        where p.id = share_events.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy analytics_events_owner_select
on public.analytics_events
for select
to authenticated
using (
    profile_id is not null and exists (
        select 1 from public.profiles p
        where p.id = analytics_events.profile_id and p.owner_id = (select auth.uid())
    )
);

create policy analytics_events_owner_insert
on public.analytics_events
for insert
to authenticated
with check (
    profile_id is not null and exists (
        select 1 from public.profiles p
        where p.id = analytics_events.profile_id and p.owner_id = (select auth.uid())
    )
);

create or replace function public.get_public_profile(requested_slug text)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
with selected_profile as (
    select p.*
    from public.profiles p
    where p.is_public
      and p.public_slug = lower(btrim(requested_slug))
    limit 1
), visibility as (
    select coalesce(jsonb_object_agg(s.field_key, s.is_public), '{}'::jsonb) as fields
    from public.profile_field_settings s
    join selected_profile p on p.id = s.profile_id
)
select jsonb_strip_nulls(
    jsonb_build_object(
        'slug', p.public_slug,
        'firstName', case when coalesce((v.fields ->> 'first_name')::boolean, false) then p.first_name end,
        'lastName', case when coalesce((v.fields ->> 'last_name')::boolean, false) then p.last_name end,
        'displayName', case when coalesce((v.fields ->> 'display_name')::boolean, false) then p.display_name end,
        'companyName', case when coalesce((v.fields ->> 'company_name')::boolean, false) then p.company_name end,
        'jobTitle', case when coalesce((v.fields ->> 'job_title')::boolean, false) then p.job_title end,
        'bio', case when coalesce((v.fields ->> 'bio')::boolean, false) then p.bio end,
        'profileImagePath', case
            when coalesce((v.fields ->> 'profile_image')::boolean, false)
            then p.public_profile_image_path
        end,
        'companyLogoPath', case
            when coalesce((v.fields ->> 'company_logo')::boolean, false)
            then p.public_company_logo_path
        end,
        'phones', coalesce((
            select jsonb_agg(
                jsonb_build_object('label', n.label, 'value', n.phone_number)
                order by n.sort_order, n.id
            )
            from public.profile_phone_numbers n
            where n.profile_id = p.id and n.is_public
        ), '[]'::jsonb),
        'emails', coalesce((
            select jsonb_agg(
                jsonb_build_object('label', e.label, 'value', e.email_address)
                order by e.sort_order, e.id
            )
            from public.profile_email_addresses e
            where e.profile_id = p.id and e.is_public
        ), '[]'::jsonb),
        'addresses', coalesce((
            select jsonb_agg(
                jsonb_build_object(
                    'label', a.label,
                    'formattedAddress', a.formatted_address,
                    'addressLine1', a.address_line_1,
                    'addressLine2', a.address_line_2,
                    'city', a.city,
                    'postalCode', a.postal_code,
                    'region', a.region,
                    'countryCode', a.country_code
                )
                order by a.sort_order, a.id
            )
            from public.profile_addresses a
            where a.profile_id = p.id and a.is_public
        ), '[]'::jsonb),
        'links', coalesce((
            select jsonb_agg(
                jsonb_build_object('type', l.link_type, 'label', l.label, 'url', l.url)
                order by l.sort_order, l.id
            )
            from public.profile_links l
            where l.profile_id = p.id and l.is_public
        ), '[]'::jsonb),
        'updatedAt', p.updated_at
    )
)
from selected_profile p
cross join visibility v;
$$;

revoke all on function public.get_public_profile(text) from public;
grant execute on function public.get_public_profile(text) to anon, authenticated;

revoke all on function public.handle_new_auth_user() from public, anon, authenticated;
revoke all on function public.initialize_profile_defaults() from public, anon, authenticated;
revoke all on function public.set_updated_at() from public, anon, authenticated;
revoke all on function public.set_profile_revision() from public, anon, authenticated;

revoke all on table public.user_accounts from anon;
revoke all on table public.profiles from anon;
revoke all on table public.profile_settings from anon;
revoke all on table public.profile_field_settings from anon;
revoke all on table public.profile_phone_numbers from anon;
revoke all on table public.profile_email_addresses from anon;
revoke all on table public.profile_addresses from anon;
revoke all on table public.profile_links from anon;
revoke all on table public.qr_settings from anon;
revoke all on table public.nfc_settings from anon;
revoke all on table public.share_events from anon;
revoke all on table public.analytics_events from anon;

grant select, insert, update, delete on table public.user_accounts to authenticated;
grant select, insert, update, delete on table public.profiles to authenticated;
grant select, insert, update, delete on table public.profile_settings to authenticated;
grant select, insert, update, delete on table public.profile_field_settings to authenticated;
grant select, insert, update, delete on table public.profile_phone_numbers to authenticated;
grant select, insert, update, delete on table public.profile_email_addresses to authenticated;
grant select, insert, update, delete on table public.profile_addresses to authenticated;
grant select, insert, update, delete on table public.profile_links to authenticated;
grant select, insert, update, delete on table public.qr_settings to authenticated;
grant select, insert, update, delete on table public.nfc_settings to authenticated;
grant select, insert on table public.share_events to authenticated;
grant select, insert on table public.analytics_events to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
    (
        'profile-assets',
        'profile-assets',
        false,
        5242880,
        array['image/jpeg', 'image/png', 'image/webp']
    ),
    (
        'public-profile-assets',
        'public-profile-assets',
        true,
        1048576,
        array['image/jpeg', 'image/png', 'image/webp']
    )
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create policy profile_assets_owner_all
on storage.objects
for all
to authenticated
using (
    bucket_id = 'profile-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'profile-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy public_profile_assets_read
on storage.objects
for select
to anon, authenticated
using (bucket_id = 'public-profile-assets');

create policy public_profile_assets_owner_write
on storage.objects
for all
to authenticated
using (
    bucket_id = 'public-profile-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'public-profile-assets'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

commit;
