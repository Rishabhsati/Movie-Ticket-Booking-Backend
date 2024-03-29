package com.rishabh.bookmyshowapplication.Service;

import com.rishabh.bookmyshowapplication.Exceptions.SeatUnavailableException;
import com.rishabh.bookmyshowapplication.Models.Movie;
import com.rishabh.bookmyshowapplication.Models.Show;
import com.rishabh.bookmyshowapplication.Models.ShowSeat;
import com.rishabh.bookmyshowapplication.Models.Theater;
import com.rishabh.bookmyshowapplication.Models.Ticket;
import com.rishabh.bookmyshowapplication.Models.User;
import com.rishabh.bookmyshowapplication.Repository.MovieRepository;
import com.rishabh.bookmyshowapplication.Repository.ShowRepository;
import com.rishabh.bookmyshowapplication.Repository.ShowSeatRepository;
import com.rishabh.bookmyshowapplication.Repository.TheaterRepository;
import com.rishabh.bookmyshowapplication.Repository.TicketRepository;
import com.rishabh.bookmyshowapplication.Repository.UserRepository;
import com.rishabh.bookmyshowapplication.Requests.BookTicketRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private TheaterRepository theaterRepository;
    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private ShowSeatRepository showSeatRepository;
    @Autowired
    private UserRepository userRepository;

    public Ticket bookTicket(BookTicketRequest bookTicketRequest) throws Exception{
//      1.  calculate the total cost of the tickets

        Movie movie = movieRepository.findMovieByMovieName(bookTicketRequest.getMovieName());
        Theater theater = theaterRepository.findById(bookTicketRequest.getTheaterId()).get();

//      1.1 find the show entity with this date and time
        Show show = showRepository.findShowByShowDateAndShowTimeAndMovieAndTheater(bookTicketRequest.getShowDate(),
                bookTicketRequest.getShowTime(),movie,theater);

        List<ShowSeat> showSeatList = showSeatRepository.findAllByShow(show);

        int totalAmount = 0;
        Boolean isAllSeatsAreAvailable = Boolean.TRUE;

        for(String seatNo : bookTicketRequest.getRequestedSeats()){
            for(ShowSeat showSeat : showSeatList){
                if(showSeat.getSeatNo().equals(seatNo)){
                    if(showSeat.isAvailable() == Boolean.FALSE) {
                        isAllSeatsAreAvailable = Boolean.FALSE;
                        break;
                    }
                    totalAmount+=showSeat.getPrice();
                }
            }
        }
        if(isAllSeatsAreAvailable==Boolean.FALSE) throw new SeatUnavailableException("The seats are Unavailable");

        // 2. make the seats booked : (only if when they are available)
        for(String seatNo : bookTicketRequest.getRequestedSeats()){
            for(ShowSeat showSeat : showSeatList){
                if(showSeat.getSeatNo().equals(seatNo)){
                    showSeat.setAvailable(Boolean.FALSE);
                }
            }
        }

//        save the ticket entity

        User user = userRepository.findUserByMobNo(bookTicketRequest.getMobNo());
        Ticket ticket = Ticket.builder().user(user).showDate(bookTicketRequest.getShowDate())
                .movieName(bookTicketRequest.getMovieName())
                .showTime(bookTicketRequest.getShowTime())
                .theaterNameAndAddress(theater.getTheaterName() + " " + theater.getAddress())
                .totalAmountPaid(totalAmount).build();

        ticket = ticketRepository.save(ticket);

//        Generate and return the ticket response
        return ticket;
    }
}
