INSERT INTO CHARTS(package_name, helm_chart, docker_registry)
VALUES ('mine.csar', 'eric-adp-app-mgr-0.0.1', 'http://www.docker.io');

INSERT INTO CHARTS_AUDIT(chart_id, timestamp, action, user_id)
VALUES (1, CURRENT_TIMESTAMP, 'onboarded', 'unknown'),
(1, CURRENT_TIMESTAMP, 'deleted', 'unknown');
