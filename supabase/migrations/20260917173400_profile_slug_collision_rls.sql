-- The uniqueness check must see private profiles too. The trigger is owned by
-- postgres, has an empty search path, and is not directly executable by API
-- roles, so SECURITY DEFINER only bypasses RLS for this narrow lookup.
alter function public.ensure_unique_profile_slug_on_insert() security definer;

revoke all on function public.ensure_unique_profile_slug_on_insert()
    from public, anon, authenticated;
