# Auth

Célfolyamatok: email+jelszó regisztráció, email megerősítés, login, session restore, logout, reset password, új jelszó és fióktörlés.

A Supabase Auth adapter a repository többi részétől külön marad. DEV buildben később debug-only local session használható; PROD-ban nem fordulhat elő ilyen kerülőút.

Google Sign-In Credential Manager adapterként kerül bekötésre, feature flag mögött. A hiányzó Google developer credential nem blokkolja az email/password Authot vagy az NFC/QR fejlesztést.

Fióktörlésnél a kliens nem használhat service role kulcsot. Ha admin jogosultság szükséges, Edge Function végzi a műveletet szerveroldalon.
