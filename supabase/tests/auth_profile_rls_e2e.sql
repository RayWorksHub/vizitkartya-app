begin;

select plan(24);

insert into auth.users (id, email, raw_user_meta_data)
values
    (
        '11111111-1111-4111-8111-111111111111',
        'vizit-e2e-accepted@example.invalid',
        jsonb_build_object(
            'privacy_policy_version', 'privacy-e2e-v1',
            'terms_version', 'terms-e2e-v1'
        )
    ),
    (
        '22222222-2222-4222-8222-222222222222',
        'vizit-e2e-gated@example.invalid',
        '{}'::jsonb
    );

select ok(
    exists (
        select 1 from public.profiles
        where user_id = '11111111-1111-4111-8111-111111111111'
    ),
    'Auth trigger bootstraps the accepted user profile'
);
select ok(
    exists (
        select 1 from public.profiles
        where user_id = '22222222-2222-4222-8222-222222222222'
    ),
    'Auth trigger bootstraps the gated user profile'
);
select ok(
    exists (
        select 1 from public.legal_acceptances
        where user_id = '11111111-1111-4111-8111-111111111111'
          and privacy_policy_version = 'privacy-e2e-v1'
          and terms_version = 'terms-e2e-v1'
    ),
    'Signup metadata is recorded as a versioned legal acceptance'
);
select ok(
    not exists (
        select 1 from public.legal_acceptances
        where user_id = '22222222-2222-4222-8222-222222222222'
    ),
    'A user without legal metadata is not silently accepted'
);

set local role authenticated;
set local request.jwt.claim.sub = '22222222-2222-4222-8222-222222222222';

select is(
    public.is_active_vizit_user(),
    true,
    'A token subject backed by auth.users is active'
);
select is(
    public.has_legal_acceptance('privacy-e2e-v1', 'terms-e2e-v1'),
    false,
    'The current document versions are initially gated'
);
select throws_ok(
    $$select public.get_my_profile_snapshot()$$,
    '42501',
    'Legal acceptance required',
    'Profile pull is blocked before legal acceptance'
);
select throws_ok(
    $$
        select public.sync_profile_snapshot(
            'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
            0,
            '{"displayName":"Blocked","contacts":[],"addresses":[],"links":[],"fieldOrder":[],"fieldVisibility":{}}'::jsonb
        )
    $$,
    '42501',
    'Legal acceptance required',
    'Profile push is blocked before legal acceptance'
);
select lives_ok(
    $$select public.accept_legal_documents('privacy-e2e-v1', 'terms-e2e-v1')$$,
    'An authenticated user can explicitly accept the configured versions'
);
select is(
    public.has_legal_acceptance('privacy-e2e-v1', 'terms-e2e-v1'),
    true,
    'The exact accepted versions are recognized'
);
select is(
    public.get_my_profile_snapshot() ->> 'status',
    'current',
    'Profile pull succeeds after legal acceptance'
);
select is(
    public.sync_profile_snapshot(
        'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
        0,
        '{"firstName":"E2E","lastName":"User","displayName":"VIZIT E2E","company":"","jobTitle":"","bio":"","displayImagePath":null,"contactImagePath":null,"logoPath":null,"publicSlug":null,"isPublic":false,"contacts":[],"addresses":[],"links":[],"fieldOrder":[],"fieldVisibility":{}}'::jsonb
    ) ->> 'status',
    'applied',
    'A legal user can atomically push a profile snapshot'
);
select is(
    (
        public.sync_profile_snapshot(
            'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
            0,
            '{"firstName":"E2E","lastName":"User","displayName":"VIZIT E2E","company":"","jobTitle":"","bio":"","displayImagePath":null,"contactImagePath":null,"logoPath":null,"publicSlug":null,"isPublic":false,"contacts":[],"addresses":[],"links":[],"fieldOrder":[],"fieldVisibility":{}}'::jsonb
        ) ->> 'serverVersion'
    )::bigint,
    1::bigint,
    'Retrying the same operation returns the original server version'
);
select is(
    (
        select sync_version from public.profiles
        where user_id = '22222222-2222-4222-8222-222222222222'
    ),
    1::bigint,
    'An idempotent retry does not increment the stored version'
);
select is(
    public.sync_profile_snapshot(
        'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
        0,
        '{"displayName":"Stale client","contacts":[],"addresses":[],"links":[],"fieldOrder":[],"fieldVisibility":{}}'::jsonb
    ) ->> 'status',
    'conflict',
    'A stale base version returns a conflict instead of overwriting'
);
select is(
    (
        public.sync_profile_snapshot(
            'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
            0,
            '{"displayName":"Stale client","contacts":[],"addresses":[],"links":[],"fieldOrder":[],"fieldVisibility":{}}'::jsonb
        ) ->> 'serverVersion'
    )::bigint,
    1::bigint,
    'Conflict retries preserve the authoritative server version'
);
select is(
    (select count(*) from public.profiles),
    1::bigint,
    'RLS exposes only the current user profile'
);
select is(
    (
        select count(*) from public.profiles
        where user_id = '11111111-1111-4111-8111-111111111111'
    ),
    0::bigint,
    'RLS hides another user profile'
);
select throws_ok(
    $$update public.profiles set display_name = 'Direct write' where user_id = auth.uid()$$,
    '42501',
    null,
    'Authenticated clients cannot bypass the versioned sync RPC'
);
select throws_ok(
    $$
        insert into public.legal_acceptances(user_id, privacy_policy_version, terms_version)
        values (auth.uid(), 'forged', 'forged')
    $$,
    '42501',
    null,
    'Authenticated clients cannot forge direct legal rows'
);
select throws_ok(
    $$select * from public.profile_sync_operations$$,
    '42501',
    null,
    'Sync operation records are not directly readable'
);

set local request.jwt.claim.sub = '33333333-3333-4333-8333-333333333333';
select is(
    public.is_active_vizit_user(),
    false,
    'A stale token subject without an Auth user is inactive'
);

set local role anon;
set local request.jwt.claim.sub = '';

select throws_ok(
    $$select * from public.profiles$$,
    '42501',
    null,
    'Anonymous clients cannot read private profiles'
);
select throws_ok(
    $$select public.has_legal_acceptance('privacy-e2e-v1', 'terms-e2e-v1')$$,
    '42501',
    null,
    'Anonymous clients cannot call the legal acceptance API'
);

select * from finish();

rollback;
