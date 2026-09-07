FROM postgis/postgis:16-3.4

# The production compose file must not depend on a runtime bind mount from the
# repository. The same idempotent script is available to PostgreSQL's first
# boot hook and to the explicit postgres-init reconciliation job.
COPY scripts/init-multiple-databases.sh /docker-entrypoint-initdb.d/init-multiple-databases.sh
COPY scripts/init-multiple-databases.sh /usr/local/bin/init-multiple-databases.sh
RUN chmod 0755 /docker-entrypoint-initdb.d/init-multiple-databases.sh /usr/local/bin/init-multiple-databases.sh
