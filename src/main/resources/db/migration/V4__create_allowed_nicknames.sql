create table if not exists allowed_nickname (
    id    bigserial not null,
    value text      not null,
    constraint allowed_nickname_pk primary key (id),
    constraint allowed_nickname_value_uq unique (value)
);

insert into allowed_nickname (value) values
('assistant'),
('class'),
('classic'),
('classy'),
('cassidy'),
('grass'),
('passage'),
('passion')
on conflict (value) do nothing;
