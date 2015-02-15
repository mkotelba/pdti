CREATE TABLE pdtiauditlog (
    id bigserial primary key,
    baseDn varchar(200) default NULL,
	pdRequestType varchar(200) default NULL,
	status varchar(200) NOT NULL,
    creationDate timestamp default NULL
);