"""Wireframe data: games and community. Read from frontend features/games, features/community and the mobile
games, community screens."""

MENU = ['Games', 'Community', 'Overview', 'Practice']

GAMES = [
    dict(name='Game Catalog', shell='site', menu=MENU, ucs='View Game Catalog, Join Game Room',
         note='On mobile the catalogue also holds the game setup (words, time limit, lives) and asks a Guest to sign in to play.',
         items=[
             ('title', 'Every new word, a new challenge', 'Heading "LEARN THROUGH GAMES" with the words Learn, Play, Grow.'),
             ('cards', 'Game', 'Wordly and Rocket. Each card offers solo play and, with the server, a room for up to 4 players. When no game exists: "No games yet. Please come back later." When the server cannot be reached: "Could not load the games."'),
             ('panel', 'Join a friend\'s room', 'Enter the room code to review and compete together.', [('field', 'Room code', '6 characters.', 'Room code'), ('button', 'Join', '')]),
             ('button2', 'Rejoin room', 'Shown when the Learner is already in a room: "You are in a game room" with "Rejoin room".'),
         ]),
    dict(name='Wordly Setup', shell='site', menu=MENU, ucs='Set Up Game',
         items=[
             ('title', 'Set up your game', 'Heading. A box "How to play": one meaning, three planets; a wrong answer or time out costs a life.'),
             ('select', 'Number of words', 'Limited (at least 5) or unlimited: play until the lives are used up or the Learner stops.'),
             ('select', 'Time for each word', '180 seconds or no limit.'),
             ('select', 'Lives for each player', 'From 3 to 6 lives. A wrong answer or a time out costs one life.'),
             ('chips', ['Easier', 'Harder'], 'Level of the words; no choice means all levels. The topics list loads from the server.'),
             ('button', 'Start', 'Starts a solo game or creates a room. A Learner already in a room sees "You are in a game room" and "Rejoin room".'),
         ]),
    dict(name='Rocket Setup', shell='site', menu=MENU, ucs='Set Up Game',
         items=[
             ('title', 'Vocabulary rocket', 'Read the Vietnamese meaning and steer the rocket into the planet that carries the right English word.'),
             ('select', 'Number of words in the game', 'At least 5.'),
             ('select', 'Time for each question', 'Seconds for each question.'),
             ('select', 'Lives for each player', 'Lives for each player.'),
             ('text', 'Sample data in use (10 sample words, not connected to the server).', 'Notice shown when the app runs without the server.'),
             ('button', 'Start', 'Starts solo or creates a room.'),
         ]),
    dict(name='Game Room', shell='site', menu=MENU, ucs='Join Game Room',
         items=[
             ('text', 'ROOM CODE', 'The code with the button "Copy room code" ("Room code copied.").'),
             ('title', 'Players in the room', 'Up to 4 seats. Invite friends by the code and get ready together.'),
             ('table', ['Player', 'Role', 'Ready'], 'Each seat shows the player, "(You)", "Room owner", "Ready", or "Free seat / Waiting for players". The owner can invite a player to leave, after confirming.'),
             ('button2', 'Edit settings', 'Owner only. Saving shows "Settings saved. Everyone must get ready again." "Settings unavailable" and "Reload room" appear on errors.'),
             ('button', 'Ready / Start', 'A player marks ready; the owner starts when everyone is ready.'),
             ('link', 'Leave room', 'Leaves the room; "Back to games".'),
         ]),
    dict(name='Wordly Play', shell='site', menu=MENU, ucs='Play Wordly',
         items=[
             ('title', 'GUESS THE MEANING WORD', 'Heading with the meaning of the word to guess.'),
             ('panel', 'Guess board', 'Rows of letters. Colours: right place, wrong place, not in the word. A hint reveals one letter and is shown "Used 1/1 hint for this word".', [('chips', ['Right place', 'Wrong place', 'None'], '')]),
             ('panel', 'Letter keyboard', 'On-screen keyboard that follows the colours; keys: letters, Submit, Delete.', [('button', 'Submit word', '')]),
             ('stats', ['Score', 'Correct words', 'Lives'], 'Score, words guessed and lives left. In a room a match ranking is shown.'),
             ('button2', 'Hint for this word', 'Reveals one letter after at least one guess (once for each word).'),
             ('button2', 'Exit game', 'Asks "Exit the game?" with "Keep playing". A pending guess can be retried: "The guess is waiting for confirmation. Retry will send the same request."'),
         ]),
    dict(name='Rocket Play', shell='site', menu=MENU, ucs='Play Rocket',
         items=[
             ('text', 'Question, time left, correct and wrong counts', 'Header of the match.'),
             ('panel', 'Sky', 'The Vietnamese meaning is shown; planets carry English words and the rocket flies into the right one ("Fly into the word that means …").', [('image', 'Rocket and planets', '')]),
             ('stats', ['Correct', 'Time', 'Score'], 'End of a round: correct answers, time, points earned and words practised.'),
             ('button', 'Play again', 'Starts a new round; "Play on" continues. A knocked-out player watches: "You are out - watching".'),
             ('button2', 'Exit game', 'Leaves with confirmation.'),
         ]),
    dict(name='Game Result', shell='site', menu=MENU, ucs='View Game Result',
         items=[
             ('title', 'Official result: GAMES / WORDLY', 'Heading.'),
             ('stats', ['Total score', 'Correct words', 'Best streak', 'Time'], 'The figures of the match.'),
             ('table', ['Rank', 'Player', 'Score', 'Lives'], 'Ranking of the players with the time to the right answer.'),
             ('button', 'Choose a game', 'Opens the game catalogue. "Back to home" goes home. "Back to the match" returns while the result can still be loaded.'),
         ]),
]

COMMUNITY = [
    dict(name='Community Feed', shell='site', menu=MENU, ucs='View Community Feed, React to Post or Comment, Comment on Post',
         note='The mobile Community tab has the filters All, Explore and Following, a search box and the button "Write a post".',
         items=[
             ('title', 'Practise and improve together with the IELTSPath community', 'Heading "IELTSPATH COMMUNITY".'),
             ('field', 'Search posts', 'Search in the community.', 'Search posts in the community'),
             ('tabs', ['Feed', 'Saved', 'Following', 'Report queue'], 'Feed, saved posts, posts of people followed, and (for staff) the queue of reports. Posts, Followers and Following lists belong to the profile.'),
             ('select', 'Filter by topic', 'All topics or one topic.'),
             ('button', 'New post', 'Opens the Write Post window.'),
             ('cards', 'Post', 'Each post shows the author, text, pictures or video, reactions, comments and a menu to report. "Load more" loads more. Empty states: "No post found. Try other keywords", "You have no posts from people you follow", "The feed is waiting for the first story".'),
         ]),
    dict(name='Post Detail', shell='site', menu=MENU, ucs='Comment on Post, React to Post or Comment, Report Content',
         note='On mobile the post opens in its own screen with a menu to edit, delete or report a comment.',
         items=[
             ('panel', 'Post', 'Author, topic, text, pictures or video, and the reactions.', [('text', 'Post content', '')]),
             ('field', 'Write a comment', 'Reply to the post or to another comment ("Replying to…"). Mentions notify the person.', 'Write a comment'),
             ('cards', 'Comment', 'Comments in order; the author can edit or delete their own; others can report it. With none: "No comments yet. Be the first to share your thoughts."'),
             ('link', 'Report', 'Opens the report window (reason, one report each per person).'),
         ]),
    dict(name='Learner Profile', shell='site', menu=MENU, ucs='View Learner Profile',
         items=[
             ('image', 'Avatar and display name', 'The public profile of a Learner. "Follow" or "Edit profile" (for the owner).'),
             ('stats', ['Posts', 'Followers', 'Following'], 'Counts; the lists open under the tabs.'),
             ('tabs', ['Posts', 'Followers', 'Following'], 'Posts written by the Learner, and the lists. With none: "You have not posted anything. Share what you are learning." or "Could not load the list."'),
             ('button', 'Write the first post', 'Shown to the owner when there is no post.'),
         ]),
    dict(name='Write Post window', shell='modal', ucs='Write Post',
         items=[
             ('select', 'Choose a topic', 'Required topic of the post.'),
             ('field', 'Post title (optional)', 'Title of the post.', 'Add a post title'),
             ('textarea', 'Post content', 'Required, what the Learner wants to share. Background colours can be chosen for text-only posts.'),
             ('button2', 'Attach pictures or videos', 'Opens the attachment panel: from the computer (JPG, PNG, WebP, GIF, up to 5 MB each, up to 4 pictures) or by link; a video link from an allowed site such as YouTube. "Uploading…" then "Upload done".'),
             ('button2', 'Emoji', 'Inserts an emoji.'),
             ('button', 'Post', 'Publishes the post (UC Write Post). Cancel asks "Cancel the post you are writing? Unposted content will be lost."'),
         ]),
    dict(name='Post Composer', shell='mobile', tabs=[], ucs='Write Post',
         items=[
             ('textarea', 'Write what you want to share…', 'Main text: "What are you studying today? Ask, share or find a study partner…".'),
             ('select', 'Choose a topic', 'Required.'),
             ('field', 'Title', 'Optional, for example "How I raised my Writing by 0.5 band".', 'Title'),
             ('field', 'Target band', 'Optional.', 'Target band'),
             ('field', 'Video link', 'Optional link such as https://youtu.be/…', 'https://youtu.be/…'),
             ('field', 'Add a picture from a link', 'Optional.', 'https://…'),
             ('chips', ['Purple', 'Night', 'Sunset', 'Ocean', 'Forest', 'Pink', 'Aurora'], 'Background colour for text-only posts.'),
             ('button', 'Post', 'Publishes. If it fails: "Could not post. Check the connection and try again." Leaving asks "Keep writing" or "Discard post".'),
         ]),
    dict(name='Bookmarked Posts', shell='mobile', tabs=[], ucs='Bookmark Post',
         items=[
             ('title', 'Saved', 'Posts the Learner bookmarked.'),
             ('cards', 'Post', 'Saved posts. With none: "No saved posts. Tap the bookmark icon under a post to keep it to read later." An error shows "Could not load saved posts" and Retry.'),
             ('link', 'Back to feed', 'Returns to the community feed.'),
         ]),
    dict(name='Community Reports', shell='staff', area='Administration', menu=['Users', 'Community reports', 'Activity log'], ucs='Moderate Community Reports',
         items=[
             ('title', 'Report queue', 'Heading "COMMUNITY MODERATION". A user without the right sees "You are not allowed to see this queue".'),
             ('table', ['Content', 'Reason', 'Reported by'], 'Open reports. While loading: "Loading reports…". With none: "No reports waiting."'),
             ('button2', 'Open content', 'Opens the post or comment that was reported.'),
             ('button2', 'Dismiss', 'Closes the report and keeps the content.'),
             ('button', 'Hide content', 'Hides the post or comment from learners; it is kept, not erased (DR-37).'),
         ]),
]

GROUPS = [('Games', GAMES), ('Community', COMMUNITY)]
