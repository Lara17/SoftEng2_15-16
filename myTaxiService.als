sig City{
	contains: some Zone
}

sig Zone{
	address: some Address,
	uses: one Queue
}

sig Taxi{
	rides: set Booking
}

sig Address{

}

sig Queue{
	include: set Taxi
}

sig User{
	booksShort: lone ShortReservation,
	booksLong: set LongReservation	
}

abstract sig Booking{

}

sig ShortReservation extends Booking{

}

sig LongReservation extends Booking{

}

fact ZonesBelongToCity{
	all z: Zone, c, c2: City | (z in c.contains and z in c2.contains) implies c=c2
	all z: Zone | z in City.contains
}

fact AddressBelongsToZones{
	all a: Address  | a in Zone.address
}

fact AddressDisjoint{
	all a: Address | one z: Zone | a in z.address
}

fact QueueOnlyInOneZone{
	all q: Queue | one z:Zone| q in z.uses
}

fact TaxiNotAvailable{
	all s: ShortReservation, t: Taxi, q: Queue | (s in t.rides) implies t not in q.include
}

fact TaxiAvailable{
	all s: ShortReservation, t: Taxi, q: Queue | (s not in t.rides) implies t in q.include
}

fact ShortReservationForOnlyOneUser{
	all s: ShortReservation, u, u2: User | (s in u.booksShort and s in u2.booksShort) implies u=u2
	all s: ShortReservation | s in User.booksShort
}

fact LongReservationForOnlyOneUser{
	all l: LongReservation, u, u2: User | (l in u.booksLong and l in u2.booksLong) implies u=u2
	all l: LongReservation | l in User.booksLong
}

fact BookingForOnlyOneTaxi{
	all b: Booking, t, t2: Taxi | (b in t.rides and b in t2.rides) implies t=t2
	all b: Booking | b in Taxi.rides
}

fact TaxiRidesOnlyOneShortReservation{
	all s, s2: ShortReservation, t: Taxi | (s!=s2 and s in t.rides) implies s2 not in t.rides
}

assert AddressInOnlyOneZone{
	all a: Address, z, z2: Zone | (a in z.address and a in z2.address) implies z=z2
}

pred show(){}

run show for 5
