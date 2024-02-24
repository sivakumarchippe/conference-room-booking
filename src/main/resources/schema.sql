DROP TABLE BOOKING_DETAILS;
DROP TABLE CONFERENCE_ROOM;
CREATE TABLE CONFERENCE_ROOM (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  name VARCHAR(250) NOT NULL,
  capacity int,
  status VARCHAR(250) DEFAULT 'AVAILABLE'
);
CREATE TABLE BOOKING_DETAILS (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  booked_by VARCHAR(250) NOT NULL,
  from_time VARCHAR(250),
  to_time VARCHAR(250),
  number_of_people int,
  CONFERENCE_ROOM_ID INT,
  FOREIGN KEY (conference_room_id) REFERENCES CONFERENCE_ROOM(id)
);

