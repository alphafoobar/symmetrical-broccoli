create sequence if not exists account_seq
    start with 1
    increment by 1;

create table if not exists accounts (
    account_id     uuid        not null,
    account_number text        not null,
    suffix         text        not null,
    customer_id    text        not null,
    customer_name  text        not null,
    nick_name      text,
    status         text        not null default 'ACTIVE',
    version        bigint      not null default 0,
    created_at     timestamptz not null,
    updated_at     timestamptz not null,
    constraint accounts_pk primary key (account_id),
    constraint accounts_account_number_suffix_uq unique (account_number, suffix),
    constraint accounts_status_check check (status in ('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED'))
);

create index if not exists accounts_customer_id_idx on accounts (customer_id);
