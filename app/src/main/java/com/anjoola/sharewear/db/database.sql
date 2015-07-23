-- Create the users table
CREATE TABLE users (
	id STRING PRIMARY KEY,
	name STRING,
	phone STRING,
	email STRING
);

-- Create the locations table
CREATE TABLE locations (
	id STRING,
	timestamp TIMESTAMP,
	lat DOUBLE,
	lng DOUBLE,
	PRIMARY KEY(id, timestamp)
);
