-- VIZIT 6.0: readable automatic profile identifiers and verified custom domains.
-- Public URLs keep using the canonical VIZIT host until a domain has been
-- verified and attached to the web deployment.

alter table public.profiles
    add column if not exists custom_domain text,
    add column if not exists custom_domain_verified boolean not null default false,
    add column if not exists custom_domain_verification_token uuid not null default gen_random_uuid();

alter table public.profiles
    drop constraint if exists profiles_custom_domain_format;

alter table public.profiles
    add constraint profiles_custom_domain_format check (
        custom_domain is null
        or (
            custom_domain = lower(custom_domain)
            and length(custom_domain) <= 253
            and custom_domain ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$'
        )
    );

create unique index if not exists profiles_custom_domain_unique
    on public.profiles (lower(custom_domain))
    where custom_domain is not null;

-- Generate a readable identifier on first insert when the client deliberately
-- leaves it empty. The existing collision recovery stays deterministic per
-- account and never silently changes an established URL during an update.
create or replace function public.ensure_unique_profile_slug_on_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    owner_token text;
    candidate text;
    fallback_slug text;
begin
    owner_token := case
        when new.owner_id is not null then pg_catalog.replace(new.owner_id::text, '-', '')
        else pg_catalog.replace(pg_catalog.gen_random_uuid()::text, '-', '')
    end;

    candidate := pg_catalog.lower(pg_catalog.btrim(pg_catalog.coalesce(new.slug, '')));
    if candidate = '' then
        candidate := pg_catalog.translate(
            pg_catalog.lower(pg_catalog.btrim(pg_catalog.coalesce(new.display_name, ''))),
            'áéíóöőúüű',
            'aeiooouuu'
        );
        candidate := pg_catalog.regexp_replace(candidate, '[^a-z0-9]+', '-', 'g');
        candidate := pg_catalog.btrim(candidate, '-');
        candidate := pg_catalog.btrim(pg_catalog.left(candidate, 50), '-');
        if pg_catalog.length(candidate) < 3 then
            candidate := 'vizit-' || pg_catalog.substr(owner_token, 1, 12);
        end if;
    end if;
    new.slug := candidate;

    if not exists (
        select 1 from public.profiles existing where existing.slug = new.slug
    ) then
        return new;
    end if;

    fallback_slug := pg_catalog.btrim(pg_catalog.left(new.slug, 40), '-')
                     || '-'
                     || pg_catalog.substr(owner_token, 1, 8);

    if exists (
        select 1 from public.profiles existing where existing.slug = fallback_slug
    ) then
        fallback_slug := 'vizit-' || owner_token;
    end if;

    while exists (
        select 1 from public.profiles existing where existing.slug = fallback_slug
    ) loop
        fallback_slug := 'vizit-'
                         || pg_catalog.replace(pg_catalog.gen_random_uuid()::text, '-', '');
    end loop;

    new.slug := fallback_slug;
    return new;
end;
$$;

revoke all on function public.ensure_unique_profile_slug_on_insert()
    from public, anon, authenticated;

drop trigger if exists profiles_unique_slug_on_insert on public.profiles;
create trigger profiles_unique_slug_on_insert
before insert on public.profiles
for each row execute function public.ensure_unique_profile_slug_on_insert();

-- Owners may request or replace a domain, but only a trusted backend process
-- may mark it verified. Changing the host always invalidates the old proof.
create or replace function public.protect_profile_custom_domain()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.custom_domain := pg_catalog.nullif(
        pg_catalog.lower(pg_catalog.btrim(new.custom_domain)),
        ''
    );

    if tg_op = 'INSERT' then
        new.custom_domain_verified := false;
        new.custom_domain_verification_token := pg_catalog.gen_random_uuid();
    elsif new.custom_domain is distinct from old.custom_domain then
        new.custom_domain_verified := false;
        new.custom_domain_verification_token := pg_catalog.gen_random_uuid();
    elsif (select auth.uid()) is not null then
        new.custom_domain_verified := old.custom_domain_verified;
        new.custom_domain_verification_token := old.custom_domain_verification_token;
    end if;

    return new;
end;
$$;

revoke all on function public.protect_profile_custom_domain()
    from public, anon, authenticated;

drop trigger if exists profiles_protect_custom_domain on public.profiles;
create trigger profiles_protect_custom_domain
before insert or update on public.profiles
for each row execute function public.protect_profile_custom_domain();

comment on column public.profiles.custom_domain is
    'Optional lowercase hostname requested by the profile owner, without scheme or path.';
comment on column public.profiles.custom_domain_verified is
    'True only after DNS and hosting verification by a trusted backend process.';
comment on column public.profiles.custom_domain_verification_token is
    'Per-domain challenge used by the hosting verification workflow.';
