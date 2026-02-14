create sequence if not exists events_seq start with 1 increment by 1;

create sequence if not exists games_seq start with 1 increment by 1;

create table if not exists events (
		event_date timestamp(6) with time zone not null,
		id bigint not null,
		description TEXT not null,
		name varchar(255) not null,
		primary key (id)
);

create table if not exists games (
		time_to_complete numeric(21,0) not null,
		event_id bigint not null,
		game_date timestamp(6) with time zone not null,
		id bigint not null,
		email varchar(255),
		first_name varchar(255) not null,
		last_name varchar(255) not null,
		primary key (id)
);

alter table if exists games
	 add constraint games_event_id_fkey
	 foreign key (event_id)
	 references events;
