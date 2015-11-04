sig City{
	contains: some Zone
}

sig Zone{
	address: some Address,
	uses: one Queue,
	encloses: some Taxi
}

sig Address{
	locates: set Booking
}

sig Taxi{
	rides: set Booking
}

sig Queue{
	include: set Taxi
}

sig User{
	booksShort: lone ShortReservation,
	booksLong: set LongReservation,
}

abstract sig Booking{

}

sig ShortReservation extends Booking{

}

sig LongReservation extends Booking{

}


//********************************************************
//						FACTS
//********************************************************

//ALL ZONES BELONGS TO A CITY
fact ZonesBelongToCity{
	all z: Zone, c, c2: City | (z in c.contains and z in c2.contains) implies c=c2
	all z: Zone | z in City.contains
}


//ALL ADDRESSES BELONG TO A ZONE
fact AddressBelongsToZones{
	all a: Address  | a in Zone.address
}


//ADDRESSES ARE DISJOINT
fact AddressDisjoint{
	all a: Address | one z: Zone | a in z.address
}


//A QUEUE CAN BE ONLY IN ONE ZONE
fact QueueOnlyInOneZone{
	all q: Queue | one z:Zone| q in z.uses
}

//IF A TAXI IS NOT AVAILABLE IN NOT IN A QUEUE
fact TaxiNotAvailable{
	all s: ShortReservation, t: Taxi, q: Queue | (s in t.rides) implies t not in q.include
}

//IN A TAXI IS AVAILABLE IS IN A QUEUE
fact TaxiAvailable{
	all t: Taxi, q: Queue | (t in q.include) implies (no s: ShortReservation  | s in t.rides)
	//all t: Taxi | (ShortReservation not in t.rides) implies (one q: Queue | t in q.include )
	//all t: Taxi | t in Queue.include
}

//ONLY ONE USER CAN BELONG TO A SHORT RESERVATION
fact ShortReservationForOnlyOneUser{
	all s: ShortReservation, u, u2: User | (s in u.booksShort and s in u2.booksShort) implies u=u2
	all s: ShortReservation | s in User.booksShort
}

//ONLY ONE USER CAN BELONG TO A SHORT RESERVATION
fact LongReservationForOnlyOneUser{
	all l: LongReservation, u, u2: User | (l in u.booksLong and l in u2.booksLong) implies u=u2
	all l: LongReservation | l in User.booksLong
}

//A SHORT RESERVATIONS IS BOUND TO ONLY ONE TAXI
fact ShortReservationForOnlyOneTaxi{
	all s: ShortReservation, t, t2: Taxi | (s in t.rides and s in t2.rides) implies t=t2
	all s: ShortReservation | s in Taxi.rides
}

//A TAXI CAN SERVE ONLY A SHORT RESERVATION
fact TaxiRidesOnlyOneShortReservation{
	all s, s2: ShortReservation, t: Taxi | (s!=s2 and s in t.rides) implies s2 not in t.rides
}

//A USER EXISTS ONLY IF HAS BOOKED SOMETHING
fact UserIfBookedSomething{
	all u: User | (u in booksShort.ShortReservation) or  (u in booksLong.LongReservation)
}

//A TAXI CAN BE ONLY IN ONE QUEUE
fact TaxisOnlyInOneQueue{
	all t: Taxi, q , q1: Queue | (t in q.include and t in q1.include) implies q=q1
}
 //A TAXI CAN BE IN ONLY ONE ZONE
fact TaxiOnlyOneZone{
	all t: Taxi, z,z2: Zone | (t in z.encloses and t in z2.encloses) implies z=z2
	all t: Taxi | t in Zone.encloses
}

//TO ONE BOOKING CORRESPONDS ONLY ONE ADDRESS
fact BookingOnlyOneAddress{
	all b: Booking, a, a2: Address | ( b in a.locates and b in a2.locates) implies a=a2
	all b: Booking | b in Address.locate
}

//A TAXI THAT IS IN A QUEUE IS IN THE ZONE RELATED TO THE QUEUE 
fact ZoneTaxiQueueAreRelate{
	all t: Taxi, q: Queue, z: Zone | (t in q.include and q in z.uses) implies (t in z.encloses)
}

//NO TAXI IS BOUND TO A LONG RESERVATION
fact LongReservationsHaveNoTaxi{
	all l: LongReservation, t: Taxi | l not in t.rides
}


//********************************************************
//						ASSERTS
//********************************************************
assert AddressInOnlyOneZone{
	all a: Address, z, z2: Zone | (a in z.address and a in z2.address) implies z=z2
}

assert ExistsCity{
	all z: Zone, t: Taxi, q: Queue | ((z in City.contains) implies some City) and ((t in q.include) implies some Queue)
}

assert  UsersHaveBookedSomething{
all u: User | (u in booksShort.ShortReservation) or  (u in booksLong.LongReservation)
}

assert TaxiInOnlyOneQueue{
	all t: Taxi, q , q1: Queue | (t in q.include and t in q1.include) implies q=q1
}

pred show(){
#ShortReservation > 1
#LongReservation > 1
}

check  TaxiInOnlyOneQueue

check AddressInOnlyOneZone
 
check ExistsCity

check UsersHaveBookedSomething

check TaxiCanExistsWithoutAQueue

run show for 5
