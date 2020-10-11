CREATE DATABASE test;

create table users (
  user_id text primary key,
  email text not null,
  age int not null
);

insert into users (user_id, email, age) values ('@todokr', 'tadokoro@example.com', 29);
insert into users (user_id, email, age) values ('@taro', 'taro@example.com', 32);