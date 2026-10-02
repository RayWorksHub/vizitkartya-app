from pathlib import Path
A=Path('app/src/main/java/hu/rayworks/vizit')
for path in [A/'v10/ui/components/Common.kt', A/'v10/ui/wizard/WizardIntro.kt', Path('ios/Vizit/App/ProfileEditor.swift')]:
    path.write_text(path.read_text().rstrip()+'\n')
p=A/'auth/AuthViewModel.kt'; s=p.read_text()
s=s.replace('    var actionState by mutableStateOf<AuthActionState>', '    var registrationEmail by mutableStateOf("")\n        private set\n\n    var actionState by mutableStateOf<AuthActionState>')
old='            requireExplicitLogin(true)\n            repositoryOrThrow().register('
assert old in s
s=s.replace(old,'            registrationEmail = email.trim()\n            repositoryOrThrow().register(')
old='                termsVersion = BuildConfig.TERMS_VERSION,\n            )'
assert s.count(old)==1
s=s.replace(old,old+'\n            requireExplicitLogin(true)')
p.write_text(s)
p=A/'ui/screens/AuthScreen.kt'; s=p.read_text()
s=s.replace('verificationEmail = email.trim()', 'verificationEmail = viewModel.registrationEmail')
p.write_text(s)
p=A/'ui/VizitRoot.kt'; s=p.read_text()
s=s.replace('authViewModel.requiresExplicitLogin || authViewModel.registrationConfirmationInProgress ->', '(authViewModel.requiresExplicitLogin || authViewModel.registrationConfirmationInProgress) && !authViewModel.debugLocalProfile ->')
p.write_text(s)
