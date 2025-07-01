DROP TABLE agent_event;

CREATE TABLE IF NOT EXISTS agent_event (
	event_id TEXT PRIMARY KEY,
	shard_id TEXT,
	username TEXT,
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


CREATE TABLE IF NOT EXISTS contact_event (
	id TEXT PRIMARY KEY,
	contact_id TEXT,
    channel TEXT,
    init_method TEXT,
    init_timestamp TEXT,


	shard_id TEXT,
	username TEXT,
	agent_status TEXT,
	event_type TEXT,

	init_contact_id TEXT,

	contact_queue TEXT,
	contact_state TEXT,
	event_timestamp TEXT,
	event_unix_timestamp BIGINT,
	full_data CLOB
);
