-- Keep the web editor and native clients on the same stable profile address.
-- A user may request a hostname, but only a trusted backend may verify it.

alter table public.profiles
  add column if not exists custom_domain text,
  add column if not exists custom_domain_verified boolean not null default false;

alter table public.profiles
  drop constraint if exists profiles_custom_domain_format;

alter table public.profiles
  add constraint profiles_custom_domain_format check (
    custom_domain is null
    or (
      custom_domain = lower(custom_domain)
      and length(custom_domain) <= 253
      and custom_domain ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$'
      and custom_domain !~ '^[0-9.]+$'
    )
  );

create unique index if not exists profiles_custom_domain_unique
  on public.profiles (lower(custom_domain))
  where custom_domain is not null;

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

drop trigger if exists profiles_protect_custom_domain on public.profiles;
create trigger profiles_protect_custom_domain
before insert or update on public.profiles
for each row execute function public.protect_profile_custom_domain();

comment on column public.profiles.custom_domain is
  'Optional lowercase hostname requested by the owner, without scheme or path.';
comment on column public.profiles.custom_domain_verified is
  'True only after DNS and hosting verification by a trusted backend process.';
