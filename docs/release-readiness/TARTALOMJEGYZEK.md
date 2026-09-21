# VIZIT – Release Readiness dokumentáció

> **Státusz:** tartalomjegyzék / dokumentációs terv. Ez a dokumentum nem fejlesztési utasítás. A cél a későbbi éles Android- és iOS-kiadáshoz szükséges megfelelési, biztonsági, jogi, QA- és üzemeltetési dokumentáció teljes struktúrájának rögzítése.

## 00 – Dokumentáció és irányítás
`00_README.md`, `01_dokumentacio_celja.md`, `02_alkalmazasi_kor.md`, `03_termekdefinicio.md`, `04_fogalmak_roviditesek.md`, `05_dokumentum_verziokezeles.md`, `06_dokumentum_tulajdonosok.md`, `07_szerepkorok_felelossegek.md`, `08_felulvizsgalati_ciklus.md`, `09_bizonyitekok_kezelese.md`, `10_eltteresek_kezelese.md`, `11_kockazatelfogadas.md`, `12_RELEASE_READINESS_MODELL.md`, `13_MASTER_REQUIREMENTS_MATRIX.md`

## 01 – Termék és release scope
`01_termek_leirasa.md`, `02_uzleti_cel.md`, `03_felhasznaloi_csoportok.md`, `04_use_case_inventory.md`, `05_funkcioleltar.md`, `06_core_funkciok.md`, `07_kritikus_funkciok.md`, `08_tamogatott_platformok.md`, `09_tamogatott_OS_verziok.md`, `10_tamogatott_eszkozok.md`, `11_regionalis_elerhetoseg.md`, `12_nyelvi_tamogatas.md`, `13_kulso_szolgaltatasok.md`, `14_release_scope.md`, `15_out_of_scope.md`, `16_known_limitations.md`, `17_RELEASE_SCOPE_CHECKLIST.md`

## 02 – Architektúra és rendszerleltár
`01_system_context.md`, `02_architecture_overview.md`, `03_mobile_architecture.md`, `04_backend_architecture.md`, `05_database_architecture.md`, `06_infrastructure_architecture.md`, `07_trust_boundaries.md`, `08_component_inventory.md`, `09_service_inventory.md`, `10_API_inventory.md`, `11_SDK_inventory.md`, `12_dependency_inventory.md`, `13_external_integration_inventory.md`, `14_data_flow_diagrams.md`, `15_network_flow_inventory.md`, `16_ARCHITECTURE_CHECKLIST.md`

## 03 – Jogi alapdokumentumok
`01_szolgaltato_adatai.md`, `02_impresszum_kovetelmenyek.md`, `03_ASZF_kovetelmenyek.md`, `04_felhasznalasi_feltetelek.md`, `05_adatkezelesi_tajekoztato.md`, `06_cookie_tracking_tajekoztatas.md`, `07_hozzajarulasi_nyilatkozatok.md`, `08_felelossegkorlatozas.md`, `09_szellemi_tulajdon.md`, `10_jogvitak_panaszkezeles.md`, `11_jogszabalyi_hivatkozasok.md`, `12_LEGAL_DOCUMENT_CHECKLIST.md`

## 04 – GDPR és adatvédelem
`01_GDPR_scope.md`, `02_adatkezelo_adatfeldolgozo_szerepek.md`, `03_adatleltar.md`, `04_szemelyes_adatok_kategoriai.md`, `05_kulonleges_adatok_vizsgalata.md`, `06_adatkezelesi_celok.md`, `07_jogalapok.md`, `08_adatminimalizalas.md`, `09_celhozkotottseg.md`, `10_pontossag.md`, `11_adatmegorzes.md`, `12_adatmegorzesi_matrix.md`, `13_hozzaferesi_jog.md`, `14_helyesbitesi_jog.md`, `15_torlesi_jog.md`, `16_korlatozasi_jog.md`, `17_adathordozhatosag.md`, `18_tiltakozasi_jog.md`, `19_hozzajarulas_visszavonasa.md`, `20_adatexport.md`, `21_fioktorles.md`, `22_adatfeldolgozok.md`, `23_aladatfeldolgozok.md`, `24_DPA_nyilvantartas.md`, `25_nemzetkozi_adattovabbitas.md`, `26_SCC_vizsgalat.md`, `27_privacy_by_design.md`, `28_privacy_by_default.md`, `29_DPIA_szuksegesseg.md`, `30_ROPA_adatkezelesi_nyilvantartas.md`, `31_adatvedelmi_incidens.md`, `32_hatosagi_bejelentes_folyamata.md`, `33_GDPR_CHECKLIST.md`

## 05 – ePrivacy, cookie, tracking és analytics
`01_tracking_inventory.md`, `02_cookie_inventory.md`, `03_mobile_identifiers.md`, `04_analytics_identifiers.md`, `05_advertising_identifiers.md`, `06_consent_requirements.md`, `07_consent_management.md`, `08_consent_withdrawal.md`, `09_analytics_without_consent.md`, `10_tracking_SDK_audit.md`, `11_tracking_retention.md`, `12_TRACKING_CHECKLIST.md`

## 06 – Fogyasztóvédelem és kereskedelmi megfelelés
`01_fogyasztovedelmi_scope.md`, `02_artranszparencia.md`, `03_rejtett_dijak.md`, `04_elofizetesek.md`, `05_automatikus_megujitas.md`, `06_lemondas.md`, `07_visszaterites.md`, `08_elallasi_jog.md`, `09_panaszkezeles.md`, `10_megteveszto_allitasok.md`, `11_hamis_ertekelesek.md`, `12_dark_patterns.md`, `13_promociok_akciok.md`, `14_digitalis_szolgaltatasok.md`, `15_CONSUMER_CHECKLIST.md`

## 07 – Kiskorúak és életkori követelmények
`01_age_scope.md`, `02_age_assurance.md`, `03_szulo_gondviselo_hozzajarulas.md`, `04_kiskoru_adatkezeles.md`, `05_kiskoru_marketing.md`, `06_child_safety_design.md`, `07_MINOR_CHECKLIST.md`

## 08 – Szellemi tulajdon és licencek
`01_source_code_ownership.md`, `02_brand_ownership.md`, `03_trademark_inventory.md`, `04_image_licenses.md`, `05_icon_licenses.md`, `06_font_licenses.md`, `07_audio_video_licenses.md`, `08_open_source_licenses.md`, `09_third_party_licenses.md`, `10_license_obligations.md`, `11_NOTICE_requirements.md`, `12_IP_LICENSE_CHECKLIST.md`

## 09 – Információbiztonság irányítás
`01_security_policy.md`, `02_security_scope.md`, `03_asset_inventory.md`, `04_asset_ownership.md`, `05_information_classification.md`, `06_risk_methodology.md`, `07_risk_assessment.md`, `08_risk_register.md`, `09_risk_treatment.md`, `10_security_roles.md`, `11_access_review.md`, `12_security_training.md`, `13_SECURITY_GOVERNANCE_CHECKLIST.md`

## 10 – Threat model és attack surface
`01_threat_model_methodology.md`, `02_assets.md`, `03_threat_actors.md`, `04_trust_boundaries.md`, `05_attack_surface.md`, `06_mobile_threats.md`, `07_backend_threats.md`, `08_API_threats.md`, `09_database_threats.md`, `10_NFC_threats.md`, `11_auth_threats.md`, `12_supply_chain_threats.md`, `13_abuse_cases.md`, `14_misuse_cases.md`, `15_threat_mitigations.md`, `16_THREAT_MODEL_CHECKLIST.md`

## 11 – Mobile app security
`01_secure_storage.md`, `02_sensitive_data_memory.md`, `03_encryption_at_rest.md`, `04_encryption_in_transit.md`, `05_key_management.md`, `06_secrets_management.md`, `07_authentication_security.md`, `08_authorization_security.md`, `09_session_security.md`, `10_token_security.md`, `11_biometric_auth.md`, `12_permissions_security.md`, `13_clipboard_security.md`, `14_screenshot_screen_recording.md`, `15_deep_links.md`, `16_app_links_universal_links.md`, `17_WebView_security.md`, `18_file_sharing.md`, `19_backup_exposure.md`, `20_logs_sensitive_data.md`, `21_root_jailbreak_risk.md`, `22_reverse_engineering.md`, `23_tampering.md`, `24_MOBILE_SECURITY_CHECKLIST.md`

## 12 – OWASP Mobile
`01_OWASP_MASVS_overview.md`, `02_MASVS_STORAGE.md`, `03_MASVS_CRYPTO.md`, `04_MASVS_AUTH.md`, `05_MASVS_NETWORK.md`, `06_MASVS_PLATFORM.md`, `07_MASVS_CODE.md`, `08_MASVS_RESILIENCE.md`, `09_MASVS_PRIVACY.md`, `10_OWASP_MASTG.md`, `11_test_mapping.md`, `12_MASVS_COMPLIANCE_MATRIX.md`

## 13 – Backend és API security
`01_API_inventory.md`, `02_endpoint_classification.md`, `03_API_authentication.md`, `04_API_authorization.md`, `05_object_level_authorization.md`, `06_function_level_authorization.md`, `07_input_validation.md`, `08_output_validation.md`, `09_rate_limiting.md`, `10_abuse_protection.md`, `11_replay_protection.md`, `12_CORS.md`, `13_error_handling.md`, `14_API_logging.md`, `15_API_versioning.md`, `16_API_deprecation.md`, `17_OWASP_API_Top10_mapping.md`, `18_API_SECURITY_CHECKLIST.md`

## 14 – Adatbázis-biztonság
`01_schema_inventory.md`, `02_data_classification.md`, `03_database_access_control.md`, `04_least_privilege.md`, `05_RLS_policies.md`, `06_encryption.md`, `07_integrity_constraints.md`, `08_audit_logging.md`, `09_migrations.md`, `10_backup_security.md`, `11_restore_security.md`, `12_data_retention.md`, `13_secure_deletion.md`, `14_DATABASE_SECURITY_CHECKLIST.md`

## 15 – Identity, authentication és account lifecycle
`01_registration.md`, `02_email_verification.md`, `03_login.md`, `04_password_policy.md`, `05_password_storage.md`, `06_password_reset.md`, `07_account_recovery.md`, `08_MFA.md`, `09_social_login.md`, `10_Apple_sign_in.md`, `11_Google_sign_in.md`, `12_session_management.md`, `13_logout.md`, `14_device_sessions.md`, `15_account_lockout.md`, `16_brute_force_protection.md`, `17_bot_protection.md`, `18_account_deletion.md`, `19_IDENTITY_CHECKLIST.md`

## 16 – Abuse, fraud és visszaélésvédelem
`01_abuse_model.md`, `02_fake_accounts.md`, `03_account_takeover.md`, `04_spam.md`, `05_automated_abuse.md`, `06_scraping.md`, `07_rate_abuse.md`, `08_contact_data_abuse.md`, `09_impersonation.md`, `10_reporting_blocking.md`, `11_ABUSE_CHECKLIST.md`

## 17 – NFC kontaktátadás és interoperabilitás
`01_NFC_scope.md`, `02_NFC_use_cases.md`, `03_sender_receiver_model.md`, `04_android_NFC_capabilities.md`, `05_iOS_NFC_capabilities.md`, `06_HyperOS_Xiaomi_capabilities.md`, `07_NDEF_requirements.md`, `08_HCE_requirements.md`, `09_vCard_contact_model.md`, `10_contact_field_mapping.md`, `11_profile_photo_handling.md`, `12_character_encoding.md`, `13_payload_size_limits.md`, `14_receiving_device_without_app.md`, `15_fallback_mechanisms.md`, `16_QR_fallback.md`, `17_link_fallback.md`, `18_permission_requirements.md`, `19_privacy_requirements.md`, `20_NFC_security_model.md`, `21_replay_attack_risk.md`, `22_spoofing_risk.md`, `23_malicious_payload_risk.md`, `24_device_compatibility_matrix.md`, `25_OS_compatibility_matrix.md`, `26_manufacturer_compatibility.md`, `27_interoperability_test_matrix.md`, `28_failure_scenarios.md`, `29_user_feedback_errors.md`, `30_NFC_RELEASE_CHECKLIST.md`

## 18 – Secure Software Development Lifecycle
`01_SSDLC_policy.md`, `02_security_requirements.md`, `03_secure_design_review.md`, `04_secure_coding_standard.md`, `05_peer_review.md`, `06_security_code_review.md`, `07_static_analysis.md`, `08_dynamic_analysis.md`, `09_dependency_analysis.md`, `10_security_testing.md`, `11_release_security_gate.md`, `12_SSDLC_CHECKLIST.md`

## 19 – Software supply chain
`01_dependency_inventory.md`, `02_SBOM.md`, `03_direct_dependencies.md`, `04_transitive_dependencies.md`, `05_package_sources.md`, `06_package_integrity.md`, `07_dependency_pinning.md`, `08_CVE_monitoring.md`, `09_dependency_updates.md`, `10_abandoned_dependencies.md`, `11_malicious_package_risk.md`, `12_build_dependencies.md`, `13_artifact_integrity.md`, `14_build_provenance.md`, `15_SUPPLY_CHAIN_CHECKLIST.md`

## 20 – Source control és GitHub security
`01_repository_inventory.md`, `02_repository_access.md`, `03_least_privilege.md`, `04_branch_strategy.md`, `05_branch_protection.md`, `06_pull_request_policy.md`, `07_code_review_policy.md`, `08_commit_integrity.md`, `09_secret_scanning.md`, `10_dependency_scanning.md`, `11_security_alerts.md`, `12_GitHub_Actions_security.md`, `13_workflow_permissions.md`, `14_repository_backup.md`, `15_GITHUB_CHECKLIST.md`

## 21 – CI/CD és build security
`01_pipeline_architecture.md`, `02_build_environment.md`, `03_reproducible_builds.md`, `04_build_permissions.md`, `05_pipeline_secrets.md`, `06_artifact_signing.md`, `07_artifact_storage.md`, `08_release_artifact_integrity.md`, `09_environment_promotion.md`, `10_production_deployment_approval.md`, `11_pipeline_audit_log.md`, `12_CICD_CHECKLIST.md`

## 22 – ISO és security framework mapping
`01_ISO_IEC_27001.md`, `02_ISO_IEC_27002.md`, `03_ISO_IEC_27017.md`, `04_ISO_IEC_27018.md`, `05_ISO_IEC_27701.md`, `06_ISO_IEC_29100.md`, `07_ISO_IEC_25010.md`, `08_ISO_IEC_12207.md`, `09_NIST_CSF_mapping.md`, `10_CIS_controls_mapping.md`, `11_OWASP_mapping.md`, `12_COMPLIANCE_CROSSWALK.md`

## 23 – Android platform
`01_supported_android_versions.md`, `02_minimum_SDK.md`, `03_target_SDK.md`, `04_package_identifier.md`, `05_manifest_review.md`, `06_permissions_inventory.md`, `07_runtime_permissions.md`, `08_exported_components.md`, `09_intents_intent_filters.md`, `10_app_links.md`, `11_network_security_config.md`, `12_local_storage.md`, `13_Android_Keystore.md`, `14_backup_rules.md`, `15_background_execution.md`, `16_battery_restrictions.md`, `17_notifications.md`, `18_NFC_platform_requirements.md`, `19_HyperOS_behavior.md`, `20_Samsung_behavior.md`, `21_Pixel_behavior.md`, `22_ANDROID_PLATFORM_CHECKLIST.md`

## 24 – Google Play release
`01_Play_Console_account.md`, `02_developer_identity.md`, `03_Play_Developer_Policies.md`, `04_app_content_declarations.md`, `05_Data_Safety.md`, `06_data_deletion_declaration.md`, `07_ads_declaration.md`, `08_content_rating.md`, `09_target_audience.md`, `10_permissions_declarations.md`, `11_app_access_instructions.md`, `12_package_ID.md`, `13_Play_App_Signing.md`, `14_upload_key.md`, `15_AAB_requirements.md`, `16_Play_Integrity.md`, `17_pre_launch_report.md`, `18_internal_testing.md`, `19_closed_testing.md`, `20_open_testing.md`, `21_production_release.md`, `22_staged_rollout.md`, `23_store_listing.md`, `24_policy_rejection.md`, `25_appeal_process.md`, `26_GOOGLE_PLAY_CHECKLIST