-- VIZIT 6.1: restore profile writes after the V6 addressing triggers.
-- NULLIF and COALESCE are PostgreSQL syntax constructs, not schema-qualified
-- functions. Prefixing them with pg_catalog caused SQLSTATE 42883 on every
-- profile INSERT/UPDATE that invoked these triggers.

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

    candidate := pg_catalog.lower(pg_catalog.btrim(coalesce(new.slug, '')));
    if candidate = '' then
        candidate := pg_catalog.translate(
            pg_catalog.lower(pg_catalog.btrim(coalesce(new.display_name, ''))),
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

create or replace function public.protect_profile_custom_domain()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.custom_domain := nullif(
        pg_catalog.lower(pg_catalog.btrim(new.custom_domain)),
        ''
    );

    if tg_op = 'INSERT' then
        new.custom_domain_verified := false;
    elsif new.custom_domain is distinct from old.custom_domain then
        new.custom_domain_verified := false;
    elsif (select auth.uid()) is not null then
        new.custom_domain_verified := old.custom_domain_verified;
    end if;

    return new;
end;
$$;

revoke all on function public.protect_profile_custom_domain()
    from public, anon, authenticated;
