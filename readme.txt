DROP TABLE agent_event;

CREATE TABLE IF NOT EXISTS agent_event (
	event_id TEXT PRIMARY KEY,
	shard_id TEXT,
	username TEXT,
	agent_arn TEXT,
	agent_status TEXT,
	event_type TEXT,
	contact_id TEXT,
	init_contact_id TEXT,
	init_method TEXT,
	contact_queue TEXT,
	contact_state TEXT,
	contact_channel TEXT,
	event_timestamp TEXT,
	event_unix_timestamp BIGINT,
	full_data CLOB
);

SELECT * FROM agent_event;


DROP TABLE contact_event;

CREATE TABLE IF NOT EXISTS contact_event (
	id TEXT PRIMARY KEY,
	shard_id TEXT,
	event_type TEXT,
	contact_id TEXT,
	channel TEXT,
	init_contact_id TEXT,
	prev_contact_id TEXT,
    init_method TEXT,
    init_timestamp TEXT,
	conn_to_sys_timestamp TEXT,
	disconn_timestamp TEXT,
	agent_arn TEXT,
	full_data CLOB
);

SELECT * FROM contact_event;