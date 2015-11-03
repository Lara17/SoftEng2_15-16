one sig City{
	contains: some Zone
}

sig Zone{
	address: some Address,
	uses: one Queue
}

sig Taxi{
}

sig Address{

}

sig Queue{
	include: some Taxi
}
/*
sig User{
	
}

abstract sig Booking{

}

sig ShortReservation extends Booking{

}

sig LongReservation extends Booking{

}
*/
fact ZonesBelongToCity{
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

fact TaxiInOnlyOneQueue{
	all t: Taxi | one q: Queue | t in q.include
}

pred show(){}

run show for 5
