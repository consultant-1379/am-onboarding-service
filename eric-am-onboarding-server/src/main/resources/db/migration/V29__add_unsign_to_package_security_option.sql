UPDATE app_packages 
SET package_security_option = 'UNSIGNED'
WHERE package_security_option ISNULL AND onboarding_state ='ONBOARDED';
