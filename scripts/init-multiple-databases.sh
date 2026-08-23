#!/bin/bash
set -e
set -u

wait_for_postgres() {
    local host_arg=""
    if [ -n "${PGHOST:-}" ]; then
        host_arg="-h $PGHOST"
    fi
    local port_arg=""
    if [ -n "${PGPORT:-}" ]; then
        port_arg="-p $PGPORT"
    fi

    echo "Waiting for PostgreSQL to be ready and accept connections..."
    until pg_isready $host_arg $port_arg -U "$POSTGRES_USER" -d postgres >/dev/null 2>&1 && \
          psql $host_arg $port_arg -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "SELECT 1" >/dev/null 2>&1; do
        sleep 1
    done
    echo "PostgreSQL is ready!"
}

create_database() {
    local database
    database=$(echo "$1" | xargs)
    [ -z "$database" ] && return

    local host_arg=""
    if [ -n "${PGHOST:-}" ]; then
        host_arg="-h $PGHOST"
    fi
    local port_arg=""
    if [ -n "${PGPORT:-}" ]; then
        port_arg="-p $PGPORT"
    fi

    if psql $host_arg $port_arg -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'" 2>/dev/null | grep -q 1; then
        echo "Database '$database' already exists"
    else
        echo "Creating database '$database'"
        psql $host_arg $port_arg -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
            -c "CREATE DATABASE \"$database\""
    fi
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    wait_for_postgres
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
        create_database "$db"
    done
    echo "Multiple databases created successfully"
fi
