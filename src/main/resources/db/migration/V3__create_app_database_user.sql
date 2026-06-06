do $$
declare
    app_user_name text := '${appDatabaseUsername}';
    app_password text := '${appDatabasePassword}';
begin
    if not exists (
        select 1
        from pg_catalog.pg_roles
        where rolname = app_user_name
    ) then
        execute format('create role %I with login password %L', app_user_name, app_password);
    else
        execute format('alter role %I with login password %L', app_user_name, app_password);
    end if;

    execute format('grant connect on database %I to %I', current_database(), app_user_name);
    execute format('grant usage on schema public to %I', app_user_name);
    execute format('grant select, insert, update, delete on all tables in schema public to %I', app_user_name);
    execute format('grant usage, select on all sequences in schema public to %I', app_user_name);
    execute format(
        'alter default privileges in schema public grant select, insert, update, delete on tables to %I',
        app_user_name
    );
    execute format(
        'alter default privileges in schema public grant usage, select on sequences to %I',
        app_user_name
    );
end
$$;
