create table if not exists workers (
    id varchar(120) primary key,
    display_name varchar(255) not null,
    labels text not null,
    capacity integer not null,
    active_runs integer not null,
    last_heartbeat timestamp not null,
    status varchar(40) not null
);

create table if not exists runs (
    id varchar(40) primary key,
    job_id varchar(120) not null,
    status varchar(40) not null,
    trigger_type varchar(40) not null,
    triggered_by varchar(255),
    worker_id varchar(120),
    queued_at timestamp not null,
    claimed_at timestamp,
    started_at timestamp,
    finished_at timestamp,
    exit_code integer,
    cancel_requested boolean not null,
    parameters_json text not null,
    message text,
    log_path text
);

create index if not exists idx_runs_job_status on runs(job_id, status);
create index if not exists idx_runs_queued on runs(status, queued_at);

create table if not exists secrets (
    name varchar(160) primary key,
    cipher_text text not null,
    updated_at timestamp not null
);
