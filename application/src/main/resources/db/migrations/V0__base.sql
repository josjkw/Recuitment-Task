CREATE TABLE chess_pieces (
    id UUID PRIMARY KEY,
    position_x INT NOT NULL,
    position_y INT NOT NULL,
    is_on_board BOOLEAN NOT NULL,
    type TEXT NOT NULL
);
