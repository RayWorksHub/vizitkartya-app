begin;

-- Trigger helpers must not inherit a caller-controlled schema search path.
alter function public.set_updated_at() set search_path = '';

-- These functions are invoked only by database triggers. Direct PostgREST RPC
-- execution would bypass their intended call sites and is not used by clients.
revoke execute on function public.set_updated_at() from public, anon, authenticated;
revoke execute on function public.increment_profile_counters() from public, anon, authenticated;
revoke execute on function public.record_signup_consent() from public, anon, authenticated;

commit;
