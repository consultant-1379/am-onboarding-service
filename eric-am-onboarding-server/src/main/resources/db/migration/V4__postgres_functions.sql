CREATE OR REPLACE FUNCTION updateAppPackages() RETURNS TRIGGER AS $update_usage_state$
   DECLARE
     packagesCount integer;
   BEGIN
     raise notice 'Operation is: %', TG_OP;
     CASE TG_OP
     WHEN 'INSERT' THEN
       raise notice 'VnfInstanceId is : %', NEW.instance_id;
       update app_packages set usage_state = 'IN_USE' where package_id = NEW.package_id;
	   RETURN NEW;
     WHEN 'DELETE' THEN
       select count(*) into packagesCount from app_pkg_instances where package_id = OLD.package_id;
         if (packagesCount = 0) THEN
           update app_packages set usage_state = 'NOT_IN_USE' where package_id = OLD.package_id;
           raise notice 'Packages count is : %', packagesCount;
         end if;
		 RETURN OLD;
         ELSE
           RAISE EXCEPTION 'Unknown Operation: "%".', TG_OP;
		   RETURN NULL;
     END CASE;
   END;
$update_usage_state$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_usage_state ON app_pkg_instances;

CREATE TRIGGER update_usage_state AFTER INSERT OR DELETE ON app_pkg_instances
FOR EACH ROW EXECUTE PROCEDURE updateAppPackages();
