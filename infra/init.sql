DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'workflow') THEN
    CREATE USER workflow WITH PASSWORD 'workflow';
END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE workflow_db TO workflow;
ALTER DATABASE workflow_db OWNER TO workflow;