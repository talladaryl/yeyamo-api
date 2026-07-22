#!/bin/bash
set -e
set -u

function create_database() {
    local database
    database=$(echo "$1" | xargs)
    [ -z "$database" ] && return

    if psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'" | grep -q 1; then
        echo "Database '$database' already exists"
    else
        echo "Creating database '$database'"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
            -c "CREATE DATABASE \"$database\""
    fi
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
        create_database "$db"
    done
    echo "Multiple databases created"
fi
