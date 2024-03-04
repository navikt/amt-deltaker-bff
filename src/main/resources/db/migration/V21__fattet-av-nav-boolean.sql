alter table vedtak rename column fattet_av_nav to fattet_av_nav_tmp;
alter table vedtak add column fattet_av_nav boolean not null default false;

update vedtak
set fattet_av_nav = true
where vedtak.fattet_av_nav_tmp is not null;

alter table vedtak drop column fattet_av_nav_tmp;