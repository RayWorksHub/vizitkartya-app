-- Keep first profile creation recoverable when a user-selected public slug is
-- already owned by another profile. Updates still fail on collisions so an
-- existing public URL is never changed silently.
create or replace function public.ensure_unique_profile_slug_on_insert()
returns trigger
language plpgsql
set search_path = ''
as $$
declare
    owner_token text;
    fallback_slug text;
begin
    new.slug := pg_catalog.lower(pg_catalog.btrim(new.slug));

    if new.owner_id is null
       or not exists (
           select 1 from public.profiles existing where existing.slug = new.slug
       ) then
        return new;
    end if;

    owner_token := pg_catalog.replace(new.owner_id::text, '-', '');
    fallback_slug := pg_catalog.left(new.slug, 40)
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
