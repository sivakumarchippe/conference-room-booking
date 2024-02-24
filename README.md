# conference-room-booking

To run the project
    - Run ConferenceRoomBookingApplication.java file

Sample Requests:

POST: http://localhost:8009/conference/book
Request:
{
"userId": "1234",
"startTime": "12:30",
"endTime": "13:00",
"numberOfPeople": 3
}

GET: http://localhost:8009/conference?startTime=14:00&endTime=14:30
