CREATE TABLE app_event (
  id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  app_id VARCHAR(64) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  finished_at TIMESTAMP,
  UNIQUE (app_id, started_at)
);

CREATE TABLE job_event (
  app_event_id INT NOT NULL,
  job_id INT NOT NULL,
  started_at TIMESTAMP NOT NULL,
  finished_at TIMESTAMP NOT NULL,
  duration_ms INT NOT NULL,
  scale_out INT NOT NULL,
  PRIMARY KEY (app_event_id, job_id),
  FOREIGN KEY (app_event_id) REFERENCES app_event (id)
)
