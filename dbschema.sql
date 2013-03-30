DROP OWNED BY cmpe275 CASCADE;
DROP ROLE IF EXISTS cmpe275;
CREATE ROLE cmpe275 LOGIN ENCRYPTED PASSWORD 'md5aa1efa18e98899a187795f467b4b3d8d'
  SUPERUSER CREATEDB CREATEROLE
   VALID UNTIL '2015-03-30 00:00:00';


DROP DATABASE IF EXISTS cmpe275;
CREATE DATABASE cmpe275
  WITH ENCODING='UTF8'
       OWNER=cmpe275
       CONNECTION LIMIT=-1;
   
\c cmpe275;

DROP SCHEMA IF EXISTS site1 CASCADE;   
CREATE SCHEMA site1
       AUTHORIZATION cmpe275;

DROP TABLE IF EXISTS site1.userdata CASCADE;      
CREATE TABLE site1.userdata
(
   user_id character varying(128) NOT NULL, 
   name character varying(128) NOT NULL, 
   city character varying(128) NOT NULL, 
   zip_code character varying(128) NOT NULL, 
   password character varying(32) NOT NULL, 
   PRIMARY KEY (user_id)
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site1.userdata
  OWNER TO cmpe275;
  
DROP TABLE IF EXISTS site1.image CASCADE; 
CREATE TABLE site1.image
(
   id serial NOT NULL, 
   file_name character varying(128) NOT NULL, 
   geom point NOT NULL, 
   data bytea NOT NULL, 
   file_type character varying(128), 
   img_time date NOT NULL, 
   user_id character varying(128) NOT NULL, 
   PRIMARY KEY (id),
   FOREIGN KEY (user_id) REFERENCES site1.userdata (user_id) ON UPDATE NO ACTION ON DELETE CASCADE
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site1.image
  OWNER TO cmpe275;  

DROP SCHEMA IF EXISTS site2 CASCADE;   
CREATE SCHEMA site2
       AUTHORIZATION cmpe275;
       
DROP TABLE IF EXISTS site2.userdata CASCADE;       
CREATE TABLE site2.userdata
(
   user_id character varying(128) NOT NULL, 
   name character varying(128) NOT NULL, 
   city character varying(128) NOT NULL, 
   zip_code character varying(128) NOT NULL, 
   password character varying(32) NOT NULL, 
   PRIMARY KEY (user_id)
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site2.userdata
  OWNER TO cmpe275;
  
DROP TABLE IF EXISTS site2.image CASCADE; 
CREATE TABLE site2.image
(
   id serial NOT NULL, 
   file_name character varying(128) NOT NULL, 
   geom point NOT NULL, 
   data bytea NOT NULL, 
   file_type character varying(128), 
   img_time date NOT NULL, 
   user_id character varying(128) NOT NULL, 
   PRIMARY KEY (id),
   FOREIGN KEY (user_id) REFERENCES site2.userdata (user_id) ON UPDATE NO ACTION ON DELETE CASCADE
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site2.image
  OWNER TO cmpe275;         

DROP SCHEMA IF EXISTS site3 CASCADE;   
CREATE SCHEMA site3
       AUTHORIZATION cmpe275;
       
DROP TABLE IF EXISTS site3.userdata CASCADE;      
CREATE TABLE site3.userdata
(
   user_id character varying(128) NOT NULL, 
   name character varying(128) NOT NULL, 
   city character varying(128) NOT NULL, 
   zip_code character varying(128) NOT NULL, 
   password character varying(32) NOT NULL, 
   PRIMARY KEY (user_id)
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site3.userdata
  OWNER TO cmpe275;
  
DROP TABLE IF EXISTS site3.image CASCADE;   
CREATE TABLE site3.image
(
   id serial NOT NULL, 
   file_name character varying(128) NOT NULL, 
   geom point NOT NULL, 
   data bytea NOT NULL, 
   file_type character varying(128), 
   img_time date NOT NULL, 
   user_id character varying(128) NOT NULL, 
   PRIMARY KEY (id),
   FOREIGN KEY (user_id) REFERENCES site3.userdata (user_id) ON UPDATE NO ACTION ON DELETE CASCADE
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site3.image
  OWNER TO cmpe275;    
  
DROP SCHEMA IF EXISTS site4 CASCADE;   
CREATE SCHEMA site4
       AUTHORIZATION cmpe275;
       
DROP TABLE IF EXISTS site4.userdata CASCADE;      
CREATE TABLE site4.userdata
(
   user_id character varying(128) NOT NULL, 
   name character varying(128) NOT NULL, 
   city character varying(128) NOT NULL, 
   zip_code character varying(128) NOT NULL, 
   password character varying(32) NOT NULL, 
   PRIMARY KEY (user_id)
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site4.userdata
  OWNER TO cmpe275;
  
DROP TABLE IF EXISTS site4.image CASCADE;   
CREATE TABLE site4.image
(
   id serial NOT NULL, 
   file_name character varying(128) NOT NULL, 
   geom point NOT NULL, 
   data bytea NOT NULL, 
   file_type character varying(128), 
   img_time date NOT NULL, 
   user_id character varying(128) NOT NULL, 
   PRIMARY KEY (id),
   FOREIGN KEY (user_id) REFERENCES site4.userdata (user_id) ON UPDATE NO ACTION ON DELETE CASCADE
) 
WITH (
  OIDS = FALSE
);
ALTER TABLE site4.image
  OWNER TO cmpe275;          