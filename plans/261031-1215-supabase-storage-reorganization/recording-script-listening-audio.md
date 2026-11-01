# Kịch bản thu âm 20 file Listening

Dùng cho plan `261031-1215-supabase-storage-reorganization`, phase 1 (nạp file còn thiếu).

## Quy định kỹ thuật

- Định dạng: **mp3**, mono, 128 kbps, 44,1 kHz hoặc 22,05 kHz.
- **Tên file đúng từng ký tự** (kể cả chữ thường và số). Tên sai là bài không phát được.
- Cắt khoảng lặng đầu và cuối; mức âm khoảng -16 LUFS; không nhạc nền.
- Đọc đúng từng câu, không thêm câu, không bỏ câu. Bài thi so khớp với transcript; sai chữ là học viên bị hiểu sai.
- Tên người nói (`Caller:`, `Guide:`…) không đọc thành lời. Đổi giọng theo người nói nếu muốn.
- Độ dài thực tế có thể lệch vài giây so với mục tiêu; không cần khớp đến từng giây.

Ví dụ chuyển định dạng (ffmpeg, nếu bản thu là wav):

```
ffmpeg -i ban-thu.wav -ac 1 -b:a 128k -ar 44100 -af loudnorm=I=-16 listening/demo/ls1.mp3
```

## Phần A: 8 file demo (`listening/demo/`)

Dùng cho các bài Listening seed của V12.

### `listening/demo/hotel.mp3`

- Length target: about **45 s**. Used by: Final test X3.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Good evening, Harbour View Hotel.
> Hello. I would like to book a room. My name is Delaney. D, E, L, A, N, E, Y.
> Thank you, Ms Delaney. How many nights?
> Four nights, from the twelfth of July.
> Certainly. Can I ask how you heard about us?
> A colleague stayed here last year. She said the rooms were quiet even though the hotel is near the station.
> That is good to hear. Breakfast is included, but parking is charged separately.

### `listening/demo/ls1.mp3`

- Length target: about **45 s**. Used by: Lesson LS1.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Good morning, Riverside Library. How can I help you?
> Hi. I would like to book a study room for my group.
> Of course. Can I have your surname, please?
> Yes, it is Thompson. T, H, O, M, P, S, O, N.
> Thank you. Which day would you like the room?
> Friday the fifteenth of May, in the afternoon, please.
> Fine. A room for up to six people costs twelve pounds for three hours.
> Twelve pounds is fine.
> And could I have a contact number?
> Sure. It is 0 7 7 0 0, 9 0 0, 3 1 4.

### `listening/demo/ls2.mp3`

- Length target: about **50 s**. Used by: Lesson LS2.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Good evening, everyone. Tonight I want to explain the new bike-sharing scheme, which starts next month.
> The council is not trying to make money from it. The main aim is to cut the number of short car journeys in the city centre, because almost half of all car trips here are under three kilometres.
> Bikes will be available at forty stations, and the first thirty minutes of every ride will be free.
> Some residents are worried about safety, so we are also building protected cycle lanes along the river.
> Finally, the scheme will be reviewed after one year. If it works well, it will be extended to the suburbs.

### `listening/demo/museum.mp3`

- Length target: about **50 s**. Used by: Practice set PS-PARA.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Welcome back to the Harbour Museum, which reopened last week after two years of repairs.
> The old building had serious problems with damp, and many objects could not be shown safely. That was the main reason for closing.
> Now, for the first time, visitors can also see the collection of ship models, which used to be kept in storage.
> Entry is still free, but we ask visitors to book a time online at weekends, when it gets very busy.
> We also hope to open a small cafe on the roof next spring.

### `listening/demo/numM.mp3`

- Length target: about **30 s**. Used by: Practice set PS-NUM.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Hi, two tickets for the seven o'clock film on Saturday, please.
> Sorry, seven o'clock is sold out. There is another showing at nine thirty.
> OK, nine thirty then. How much is that?
> Two adults, that is eighteen pounds fifty altogether.
> You are in row F, seats twelve and thirteen.

### `listening/demo/spellM.mp3`

- Length target: about **35 s**. Used by: Practice set PS-SPELL.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> City Fitness, how can I help?
> I want to join. My surname is Okafor, O, K, A, F, O, R.
> And the company you work for?
> Brightwell. B, R, I, G, H, T, W, E, L, L. Two Ls at the end.
> Great. And your email username?
> It is J, A, Y... sorry, J, A, Y, E, then the number eighty-eight.

### `listening/demo/tour.mp3`

- Length target: about **40 s**. Used by: Final test X4.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Hello and welcome. The walking tour used to start at ten, but it now leaves at ten thirty from the fountain in Market Square.
> My name is Ms Fenn, that is F, E, N, N, and I will be your guide for about two hours.
> We focus on the history of the old wool trade rather than on famous buildings, so you will visit several workshops that most tourists never see.
> Please do not bring large bags. A small backpack is fine.

### `listening/demo/trapM.mp3`

- Length target: about **30 s**. Used by: Practice set PS-TRAP.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Shall we meet on Thursday to plan the trip?
> Thursday is difficult for me. Could we make it Friday instead?
> Friday is fine. Let us meet at the station.
> Actually, the cafe next to the library is quieter. Let us go there.
> Good idea. I will bring some sandwiches.
> No need, the cafe does lunch. Just bring the map.

## Phần B: 12 file luyện tập (`listening/practice/`)

Dùng cho ba đề PT-LISTENING-01, 02, 03 (mỗi đề bốn phần). Số giây lấy từ `V35`.

### `listening/practice/pt-listening-01-s1.mp3`

- Length target: about **50 s**. Used by: Practice test PT-LISTENING-01, section 1.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Hello, Riverside Leisure Centre.
> Hi, I would like to enrol my daughter in a swimming course.
> Certainly. What is her name?
> Emily Parker. That is P, A, R, K, E, R.
> And how old is she?
> She is nine.
> We have a beginners class on Wednesday at four thirty. It costs forty-five pounds for six weeks.
> Perfect. Can I pay by card?
> Yes. And a contact number, please?
> It is 07700 900 482.

### `listening/practice/pt-listening-01-s2.mp3`

- Length target: about **48 s**. Used by: Practice test PT-LISTENING-01, section 2.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Welcome to the Greenfield Farm Park. Let me explain how the day will work.
> The farm opens at nine thirty, but the animal feeding starts at eleven, so please arrive early if you want to take part.
> The tractor ride is the most popular activity, and it leaves from the main barn every hour.
> If you are bringing young children, the best place for a picnic is beside the lake, because it has benches and shade.
> Please do not feed the animals anything you have brought yourself. Special food is on sale at the shop for two pounds a bag.
> The park closes at five, and the last tractor ride leaves at four.

### `listening/practice/pt-listening-01-s3.mp3`

- Length target: about **59 s**. Used by: Practice test PT-LISTENING-01, section 3.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> So, your group project is on renewable energy. Who will do which part?
> I would like to do the research on solar power, because I studied it last year.
> Then I will take wind power. I have already found some good articles.
> Someone also needs to design the presentation slides.
> I am not good at design. Why don't you do the slides, Maya, and I will write the final report?
> Fine. What about the survey of students?
> I suggest Jon carries it out, since he has the free time on Fridays.
> OK. We should also decide when to practise together. Thursday afternoon is best for me.
> Thursday works. And the deadline is the twentieth of March.

### `listening/practice/pt-listening-01-s4.mp3`

- Length target: about **39 s**. Used by: Practice test PT-LISTENING-01, section 4.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Today I want to talk about honeybees. A single hive may contain fifty thousand bees in summer, but only one queen. The queen lays up to two thousand eggs a day. Worker bees, all of them female, collect nectar and pollen, and they also keep the hive at a steady temperature by beating their wings. To tell other bees where food is, a worker performs a waggle dance, and the angle of the dance shows the direction of the flowers. In winter, the bees form a tight cluster to stay warm and feed on the honey they stored.

### `listening/practice/pt-listening-02-s1.mp3`

- Length target: about **41 s**. Used by: Practice test PT-LISTENING-02, section 1.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Good afternoon, Harbour View Hotel.
> Hello, I would like to book a double room for three nights.
> Certainly. What name is it under?
> Delaney. D, E, L, A, N, E, Y.
> And when will you arrive?
> On the twelfth of June.
> A double room with a sea view is ninety pounds a night.
> That is fine. Does that include breakfast?
> Yes, breakfast is served from seven until ten.

### `listening/practice/pt-listening-02-s2.mp3`

- Length target: about **42 s**. Used by: Practice test PT-LISTENING-02, section 2.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Welcome to the City Science Museum. We will begin on the ground floor, where the space exhibition is.
> The space exhibition is the newest part of the museum, and it opened only last spring.
> Please note that photography is allowed everywhere except in the planetarium.
> The planetarium shows start every forty minutes, and tickets are free but must be collected at the desk.
> On the second floor you will find the cafe, which is open until four o'clock.
> Lockers cost one pound, and you get the coin back when you return the key.

### `listening/practice/pt-listening-02-s3.mp3`

- Length target: about **48 s**. Used by: Practice test PT-LISTENING-02, section 3.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> You need to pick a research topic by Friday. Priya, have you decided?
> I am interested in how social media affects sleep, and I will collect the data by interviewing students.
> I would like to study noise in open-plan offices. I will use a questionnaire.
> Good. Tom, you should check the university's ethics rules before you begin.
> Dr Hughes, could you recommend a book on research methods?
> I will email you a reading list this afternoon.
> When should we book the library study room?
> The best time is Tuesday morning, as it is usually empty.

### `listening/practice/pt-listening-02-s4.mp3`

- Length target: about **36 s**. Used by: Practice test PT-LISTENING-02, section 4.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> A green roof is a roof covered with soil and plants. Such roofs can reduce the temperature inside a building by several degrees in summer, because the plants provide natural insulation. They also absorb rainwater, which reduces the risk of flooding in heavy storms. In some cities, roof gardens are used to grow vegetables, and they provide a habitat for insects such as bees. The main drawback is weight: the roof must be strong enough to support the soil, so the initial cost is higher.

### `listening/practice/pt-listening-03-s1.mp3`

- Length target: about **39 s**. Used by: Practice test PT-LISTENING-03, section 1.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> FitLife Club, how can I help?
> I would like to become a member. My name is Okafor, O, K, A, F, O, R.
> And what kind of membership? We have monthly or yearly.
> Yearly, please.
> A yearly membership is two hundred and forty pounds.
> Can I start next Monday?
> Of course. And which sport are you most interested in?
> Swimming, mainly.

### `listening/practice/pt-listening-03-s2.mp3`

- Length target: about **42 s**. Used by: Practice test PT-LISTENING-03, section 2.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Thank you all for coming. The festival will take place on the last Saturday of September.
> Last year it rained, so this year we have booked the community hall as a back-up.
> The main stage will be in the park, and the music begins at two in the afternoon.
> Volunteers are needed to help with parking. Please speak to Mrs Clark if you can help.
> Food stalls will be near the entrance, and each stall must pay a fee of fifteen pounds.
> Posters will be printed this week, and we need someone to put them up in shops.

### `listening/practice/pt-listening-03-s3.mp3`

- Length target: about **46 s**. Used by: Practice test PT-LISTENING-03, section 3.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Lena, Sam, I have read both your essays. Lena, your introduction is very clear.
> Thank you. I spent a long time on the conclusion.
> The conclusion is too short, I am afraid. Sam, you need to check your references.
> I used a lot of websites. Should I use more books?
> Yes, at least three books. And Lena, your grammar is excellent, but you should avoid long sentences.
> I will try. When is the final version due?
> By next Wednesday at noon.

### `listening/practice/pt-listening-03-s4.mp3`

- Length target: about **35 s**. Used by: Practice test PT-LISTENING-03, section 4.
- Read the lines below in order, one voice per speaker (labels are not read aloud).

> Early people told the time by the position of the sun, and the first sundials appeared about five thousand years ago. Later, water clocks measured time by the flow of water, and they worked at night as well. Mechanical clocks appeared in Europe in the fourteenth century, and they used a weight to drive the hands. In 1656 a scientist invented the pendulum clock, which was far more accurate. Today, atomic clocks lose less than one second in millions of years.

---

Tổng: 8 file demo, 12 file luyện tập (20 khóa).
