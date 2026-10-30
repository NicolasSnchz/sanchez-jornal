-- Esquema inicial de Jornal. Compatible con PostgreSQL (produccion) y H2 en modo PostgreSQL (desarrollo y pruebas).
-- Hibernate solo valida este esquema (ddl-auto=validate); nunca lo crea ni lo modifica.

create table users (
    id                    uuid         not null primary key,
    version               bigint,
    document_type         varchar(20)  not null,
    document_number       varchar(20)  not null,
    first_name            varchar(60)  not null,
    last_name             varchar(60)  not null,
    birth_date            date         not null,
    gender                varchar(20)  not null,
    phone                 varchar(20)  not null,
    email                 varchar(120) not null,
    city                  varchar(60)  not null,
    password_hash         varchar(100) not null,
    active                boolean      not null,
    failed_login_attempts integer      not null default 0,
    locked_until          timestamp,
    created_at            timestamp    not null,
    constraint uk_users_email unique (email),
    constraint uk_users_document unique (document_type, document_number)
);

create table foremen (
    user_id      uuid         not null primary key,
    company_name varchar(120),
    constraint fk_foremen_user foreign key (user_id) references users (id)
);

create table workers (
    user_id             uuid        not null primary key,
    trade               varchar(20) not null,
    years_of_experience integer     not null,
    neighborhood        varchar(60) not null,
    has_own_tools       boolean     not null,
    arl_valid_until     date        not null,
    accepting_offers    boolean     not null,
    emergency_phone     varchar(20) not null,
    constraint fk_workers_user foreign key (user_id) references users (id),
    constraint ck_workers_experience check (years_of_experience >= 0)
);

create table construction_sites (
    id                 uuid         not null primary key,
    version            bigint,
    foreman_id         uuid         not null,
    name               varchar(120) not null,
    description        varchar(500),
    address            varchar(150) not null,
    neighborhood       varchar(60)  not null,
    city               varchar(60)  not null,
    building_permit    varchar(40),
    start_date         date         not null,
    estimated_end_date date         not null,
    status             varchar(20)  not null,
    created_at         timestamp    not null,
    constraint fk_sites_foreman foreign key (foreman_id) references foremen (user_id),
    constraint ck_sites_dates check (estimated_end_date >= start_date)
);
create index ix_sites_foreman on construction_sites (foreman_id);

create table work_fronts (
    id                  uuid          not null primary key,
    version             bigint,
    site_id             uuid          not null,
    task                varchar(100)  not null,
    required_trade      varchar(20)   not null,
    openings            integer       not null,
    offered_daily_wage  numeric(12,0) not null,
    start_date          date          not null,
    end_date            date          not null,
    start_time          time          not null,
    end_time            time          not null,
    lunch_break_minutes integer       not null,
    constraint fk_fronts_site foreign key (site_id) references construction_sites (id),
    constraint ck_fronts_openings check (openings > 0),
    constraint ck_fronts_wage check (offered_daily_wage > 0),
    constraint ck_fronts_dates check (end_date >= start_date),
    constraint ck_fronts_times check (end_time > start_time)
);
create index ix_fronts_site on work_fronts (site_id);

create table work_front_days (
    work_front_id uuid        not null,
    day_of_week   varchar(10) not null,
    constraint pk_work_front_days primary key (work_front_id, day_of_week),
    constraint fk_front_days_front foreign key (work_front_id) references work_fronts (id)
);

create table assignments (
    id                    uuid          not null primary key,
    version               bigint,
    worker_id             uuid          not null,
    front_id              uuid          not null,
    status                varchar(20)   not null,
    invited_at            timestamp     not null,
    responded_at          timestamp,
    start_date            date          not null,
    actual_end_date       date,
    daily_wage            numeric(12,0) not null,
    worker_rating         integer,
    worker_rating_comment varchar(300),
    foreman_rating        integer,
    cancellation_reason   varchar(300),
    constraint fk_assignments_worker foreign key (worker_id) references workers (user_id),
    constraint fk_assignments_front foreign key (front_id) references work_fronts (id),
    constraint ck_assignments_wage check (daily_wage > 0),
    constraint ck_assignments_worker_rating check (worker_rating between 1 and 5),
    constraint ck_assignments_foreman_rating check (foreman_rating between 1 and 5)
);
create index ix_assignments_worker on assignments (worker_id);
create index ix_assignments_front on assignments (front_id);

create table work_days (
    id             uuid         not null primary key,
    version        bigint,
    assignment_id  uuid         not null,
    work_date      date         not null,
    check_in_time  time         not null,
    check_out_time time         not null,
    break_minutes  integer      not null,
    task_performed varchar(200) not null,
    notes          varchar(500),
    registered_at  timestamp    not null,
    confirmed_at   timestamp,
    disputed_at    timestamp,
    dispute_reason varchar(300),
    corrected_at   timestamp,
    constraint fk_work_days_assignment foreign key (assignment_id) references assignments (id),
    constraint uk_work_days_date unique (assignment_id, work_date),
    constraint ck_work_days_times check (check_out_time > check_in_time),
    constraint ck_work_days_break check (break_minutes >= 0)
);

create table payments (
    id                    uuid          not null primary key,
    version               bigint,
    payment_type          varchar(20)   not null,
    assignment_id         uuid          not null,
    receipt_number        varchar(20)   not null,
    paid_at               timestamp     not null,
    amount                numeric(12,0) not null,
    payment_method        varchar(20)   not null,
    transaction_reference varchar(40),
    confirmed_at          timestamp,
    cancelled_at          timestamp,
    cancellation_reason   varchar(300),
    reason                varchar(200),
    requested_by_worker   boolean,
    period_start          date,
    period_end            date,
    final_settlement      boolean,
    constraint fk_payments_assignment foreign key (assignment_id) references assignments (id),
    constraint uk_payments_receipt unique (receipt_number),
    constraint ck_payments_amount check (amount > 0),
    constraint ck_payments_type check (payment_type in ('ADVANCE', 'SETTLEMENT'))
);
create index ix_payments_assignment on payments (assignment_id);

create sequence receipt_number_seq start with 1 increment by 1;
