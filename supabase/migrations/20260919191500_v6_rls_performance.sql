-- Cache auth.uid() once per statement in the legacy public-schema policies.
-- Permissions and row predicates are otherwise unchanged.

drop policy if exists profiles_public_or_owner_read on public.profiles;
create policy profiles_public_or_owner_read on public.profiles
for select to public
using (is_public = true or owner_id = (select auth.uid()));

drop policy if exists profiles_owner_insert on public.profiles;
create policy profiles_owner_insert on public.profiles
for insert to authenticated
with check (owner_id = (select auth.uid()));

drop policy if exists profiles_owner_update on public.profiles;
create policy profiles_owner_update on public.profiles
for update to authenticated
using (owner_id = (select auth.uid()))
with check (owner_id = (select auth.uid()));

drop policy if exists profiles_owner_delete on public.profiles;
create policy profiles_owner_delete on public.profiles
for delete to authenticated
using (owner_id = (select auth.uid()));

drop policy if exists social_links_public_or_owner_read on public.social_links;
create policy social_links_public_or_owner_read on public.social_links
for select to public
using (exists (
    select 1 from public.profiles
    where profiles.id = social_links.profile_id
      and (profiles.is_public = true or profiles.owner_id = (select auth.uid()))
));

drop policy if exists social_links_owner_insert on public.social_links;
create policy social_links_owner_insert on public.social_links
for insert to authenticated
with check (exists (
    select 1 from public.profiles
    where profiles.id = social_links.profile_id
      and profiles.owner_id = (select auth.uid())
));

drop policy if exists social_links_owner_update on public.social_links;
create policy social_links_owner_update on public.social_links
for update to authenticated
using (exists (
    select 1 from public.profiles
    where profiles.id = social_links.profile_id
      and profiles.owner_id = (select auth.uid())
))
with check (exists (
    select 1 from public.profiles
    where profiles.id = social_links.profile_id
      and profiles.owner_id = (select auth.uid())
));

drop policy if exists social_links_owner_delete on public.social_links;
create policy social_links_owner_delete on public.social_links
for delete to authenticated
using (exists (
    select 1 from public.profiles
    where profiles.id = social_links.profile_id
      and profiles.owner_id = (select auth.uid())
));

drop policy if exists events_owner_read on public.profile_events;
create policy events_owner_read on public.profile_events
for select to authenticated
using (exists (
    select 1 from public.profiles
    where profiles.id = profile_events.profile_id
      and profiles.owner_id = (select auth.uid())
));

drop policy if exists consents_owner_read on public.privacy_consents;
create policy consents_owner_read on public.privacy_consents
for select to authenticated
using (user_id = (select auth.uid()));
