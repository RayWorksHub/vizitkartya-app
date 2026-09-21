-- The legacy profiles table is publicly readable for public cards, therefore
-- it must not contain a reusable domain-verification secret. Domain ownership
-- is verified by the trusted hosting workflow instead.

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

alter table public.profiles
    drop column if exists custom_domain_verification_token;
