import kotlin.random.Random

interface Role {
val roleTitle: String;
    fun getEnemy(heroes: MutableList<Hero>) : String
}
interface Subscriber{
    fun update(dodged: Boolean, hp: Int, numberOfCards: Int){}
}
interface Publisher{
    fun subscribe(s: Subscriber)
    fun notifySubscribers(dodged: Boolean, hp: Int, numOfCards: Int)
    fun removeSubscriber(s: Subscriber)
}

class Monarch() : Publisher, Role{
override val roleTitle = "Monarch"
    var subscribers = mutableListOf<Subscriber>()
    override fun subscribe(s: Subscriber) {
        subscribers.add(s)
    }
    override fun removeSubscriber(s: Subscriber) {
        subscribers.remove(s)
    }
    override fun notifySubscribers(dodged: Boolean, hp: Int, numOfCards: Int) {
        for(s in subscribers){
            s.update(dodged, hp, numOfCards)
        }
    }

    override fun getEnemy(heroes: MutableList<Hero>):String {
    for(m in heroes){
        if(m.roleTitle =="Rebel"&&m.alive){
            return "Rebel"
        }
    }
    return "Traitors"
}
}

class Minister() :  Role{
    override val roleTitle = "Minister"

    override fun getEnemy(heroes: MutableList<Hero>):String {
        for(m in heroes){
            if(m.roleTitle =="Rebel"&&m.alive){
                return "Rebel"
            }
        }
        return "Traitors"
    }
}

class Rebel() :   Role{
    var danger_level : Int = 0
    override val roleTitle = "Rebel"

    override fun getEnemy(heroes: MutableList<Hero>): String {
        for (m in heroes) {
            if ((m as Hero).roleTitle == "Monarch"&&m.alive) {
                return "Monarch"
            }
        }
        return "Minister"
    }
}

class Traitors() :  Role {
    override val roleTitle = "Traitors"

    override fun getEnemy(heroes: MutableList<Hero>): String {
        for (m in heroes) {
            if (m.roleTitle == "Rebel"&&m.alive) {
                return "Rebel"
            }
        }
        for (m in heroes) {
            if ((m as Hero).roleTitle == "Monarch"&&m.alive) {
                return "Monarch"
            }
        }
        return "Minister"
    }
}

interface Handler{
    fun setNext(h: Handler);
    fun handle(): Boolean;
}

interface Command{
    fun execute():Boolean{
        return Random.nextBoolean()==false
    }
}//to abandon round

interface Strategy{
    fun selectCardToDiscard()
    fun playNextCard(): Boolean
    var state: State?
    fun changeState(s:State){
        state = s
    }
}//next card and discard and change state

interface State{
    fun discard(h: Hero)
    fun recommandCardToDiscard()
}

open class BasicStrategy(val h: Hero) :Strategy{
    override var state: State?=null
    override fun selectCardToDiscard(){
        println("Selecting a card to discard...")
        h.removeCard(h.Cards[recommendCardToDiscard()])
        println("Current HP is " + h.hp +", now have " + h.Cards.size)
    }

    override fun playNextCard(): Boolean {
        if(h.canAttack) {
            h.attack(heroes.heroes)
        }else return false
        h.canAttack = false
        return true
    }

    open fun recommendCardToDiscard() :Int{
        return 0
    }//Discard the first Card
}//Attack and discard the first two


class HealthyState: State{
    lateinit var h:Hero
    lateinit var reference: Strategy;

    fun setStrategy(s :Strategy){
        reference = s;
    }
    override fun discard(h:Hero) {
        println("Selecting a card to discard...")
        println("Healthy, keep attack card rather than dodge card")
        if(h.Cards.any { it is AttackCard}){
            var atcard = h.Cards.find { it is AttackCard}
            h.Cards.remove(atcard)
        }else h.Cards.removeAt(0)
        println("Current HP is " + h.hp +", now have " + h.Cards.size)
    }

    companion object {
        fun createHealthStateWithStrategy(strategy: Strategy) : State {
            val hs = HealthyState()
            hs.setStrategy(strategy)
            return hs
        }
    }
}

class UnhealthyState: State{
    lateinit var h:Hero
    lateinit var reference: Strategy;

    fun setStrategy(s :Strategy){
        reference = s;
    }
    override fun discard(h: Hero) {
        println("Selecting a card to discard...")
        println("Unhealthy, keep dodge card rather than attack card")
        if(h.Cards.any { it is AttackCard}){
            var dgcard = h.Cards.find { it is DodgeCard}
            h.Cards.remove(dgcard)
        }else h.Cards.removeAt(0)
        println("Current HP is " + h.hp +", now have " + h.Cards.size)
    }

    companion object {
        fun createUnhealthStateWithStrategy(strategy: Strategy) : State {
            val uhs = UnhealthyState()
            uhs.setStrategy(strategy)
            return uhs
        }
    }
}

interface Card {
    var name: String
    var suit: String
    var number: Int //1 - 13 , 11 = J , 12 = Q , 13 = K , 1 = A
    fun ability(target: String)
}

abstract class BasicCard(): Card{
    override var name = "Basic Card"
    override var suit: String = CardFactory.getRandomSuit()
    override var number : Int = CardFactory.getRandomNumber()
    override fun ability(target : String) {}
}

class AttackCard(): BasicCard(){
    override var name = "attack"
    override fun ability(target : String) {
        println("Use attack card to attack, and the target is $target")
    }
}
class DodgeCard(): BasicCard(){
    override var name = "dodge"
    override fun ability(target : String) {
        println ("Spend one card to dodge.")
    }
}
class PeachCard(): BasicCard(){
    override var name = "peach"
    override fun ability(target : String) {}
    fun ability(hero :Hero,card : PeachCard){
        hero.hp = hero.hp+1
        println("${hero.name} spent 1 card to heal him/herself. Current hp is ${hero.hp}")
        hero.Cards.remove(card)
    }
}

abstract class TacticsCard(): Card{
    override var name = "TacticsCard"
    override var suit: String = CardFactory.getRandomSuit()
    override var number : Int = CardFactory.getRandomNumber()
    override fun ability(target : String){}
}

class BarbariansCard(): TacticsCard(){
    override var name = "barbarians"
    override fun ability(target : String) {
        for (i in heroes.getSurvialHero()){
            //If user == target, jump to next one
            if(i.name == target) { continue }
            if(i.beingTacticsed()){ continue }
            //If there is Attack, jump to the next one
            if (i.Cards.any{ it is AttackCard }){
                i.Cards.find{ it is AttackCard}?.let { i.removeCard(it) }
                println("${i.name} used an Attack card --[Barbarians Assault]")
                continue
            }
            if(i is Guan_Yu && i.Cards.any{ CardFactory.getColor(it.suit) == "red" }){
                i.Cards.find{ CardFactory.getColor(it.suit) == "red"}?.let { i.removeCard(it) }
                println("${i.name} used a red card instead of attack card --[Barbarians Assault] [Guan Yu]")
                continue
            }
            if(i is Zhang_Yun && !i.Cards.any{ it is AttackCard } && i.Cards.any { it is DodgeCard }){
                i.Cards.remove(DodgeCard())
                println("Used a Dodge card as Attack card - [Dragon Courage]")
                continue
            }
            //No Attack
            println("${i.name} don't have any Attack card, ${i.name} reduced one hp. -[Barbarians Assault]")
            //Default 1
            i.reduceHP(heroes.damageState)
        }
    }
}
class HailofarrowsCard(): TacticsCard(){
    override var name = "hailofarrow"
    override fun ability(target : String) {
        for (i in heroes.getSurvialHero()){
            //If user == target, jump to next one
            if(i.name == target) { continue }
            if(i.beingTacticsed()){ continue }
            //Have armor
            if(i.armor != null){
                //true -> dodge, false -> nothing happen
                if(i.armor!!.ability()){
                    println("${i.name} used a Dodge card --[Hail of Arrows]")
                    continue
                }
            }
            //Have Dodge, jump to next one
            if (i.Cards.any{ it is DodgeCard }){
                i.Cards.find{ it is DodgeCard}?.let { i.removeCard(it) }
                println("${i.name} used a Dodge card --[Hail of Arrows]") //used a Dodge card --[Hail of Arrows]
                continue
            }
            if(i is Zhen_Ji && i.checkdodge()){
                i.Cards.find{ it is DodgeCard}?.let { i.removeCard(it) }
                println("${i.name} used a Dodge card --[Hail of Arrows]") //used a Dodge card --[Hail of Arrows]
                continue
            }
            if(i is Zhang_Yun && i.checkdodge()){
                i.Cards.find{ it is DodgeCard}?.let { i.removeCard(it) }
                println("${i.name} used a Dodge card --[Hail of Arrows]") //used a Dodge card --[Hail of Arrows]
            }
            //No Dodge
            println("${i.name} don't have any Dodge card, ${i.name} reduced one hp. -[Hail of Arrows]")
            //Default 1
            i.reduceHP(heroes.damageState)
        }
    }
}
class OathofpeachCard(): TacticsCard(){
    override var name = "oathofpeach"
    override fun ability(target : String) {
        for (h in  heroes.getSurvialHero()){
            if(h.hp >= h.maxHP){ continue }
            h.hp++
        }
        println("Every hero gain 1 health -[Oath of Peach Garden]")
    }
}

class Harvest(): TacticsCard(){
    override var name = "harvest"
    override fun ability(target : String) {
        // Created a list to store the revealed cards.
        val revealcard = mutableListOf<Card>()


//        Reveal X cards from the top of the Library.
        repeat( heroes.getSurvialHero().size){
            revealcard.add(CardFactory.genCards())
        }
//         A for loop to print the reveal cards.
        var revealString = "The revealed cards are: "
        for(i in revealcard){
            revealString += "${i.name}, "
        }
        println(revealString)

//      Start from the User, add the card in orderly.
        for (i in  heroes.getSurvialHero()){
            i.Cards.add(revealcard.get(0))
            println("${i.name} was added ${revealcard.get(0).name}. -[Harvest]")
            revealcard.removeAt(0)
        }

    }
}
class Sleightofhand(): TacticsCard(){
    override var name = "sleightofhand"
    override fun ability(target : String) {
        val targetHero : Hero =  heroes.getSurvialHero().find{ it.name == target } ?: return
        repeat(2){targetHero.Cards.add(CardFactory.genCards())}
        println("${targetHero.name} draw two cards. -[Sleight of Hand]")
    }
}

class BurnBridges(): TacticsCard(){
    override var name = "burnbridges"
    override fun ability(target : String) {
        //With this target>continue, without >go back
        var targetHero : Hero = heroes.getSurvialHero().find{ it.name == target } ?: return
        if(targetHero.beingTacticsed()){ return }
        //If there is equipment, remove it first
        //+1 Monnts
        if(targetHero.increasedMounts != null){
            println("${targetHero!!.name} was removed 1 equipment -- ${targetHero.increasedMounts!!.name}. -[BurnBridges]")
            targetHero.increasedMounts = null
            return
        }
        //Armor
        if(targetHero.armor != null){
            println("${targetHero!!.name} was removed 1 equipment -- ${targetHero.armor!!.name}. -[BurnBridges]")
            targetHero.armor = null
            return
        }
        //Weapon
        if(targetHero.weapon != null){
            println("${targetHero!!.name} was removed 1 equipment -- ${targetHero.weapon!!.name}. -[BurnBridges]")
            targetHero.weapon = null
            return
        }
        //-1 Monnts
        if(targetHero.reducedMounts != null){
            println("${targetHero!!.name} was removed 1 equipment -- ${targetHero.reducedMounts!!.name}. -[BurnBridges]")
            targetHero.reducedMounts = null
            return
        }

        var randRemoveCardIndex : Int = Random.nextInt(0, targetHero.Cards.size)
        var removedcard : Card = targetHero.Cards.get(randRemoveCardIndex)
        targetHero.removeCard(removedcard)
        println("${targetHero!!.name} was removed 1 card -- ${removedcard.name}. -[BurnBridges]")
    }
}

class Duel(): TacticsCard(){
    override var name = "duel"
    override fun ability(target : String) {}
    fun ability(target : String,user : String) {
        //With this target>continue, without >go back
        //Attacker
        var userHero : Hero =  heroes.getSurvialHero().find{ it.name == user } ?: return
        //Be attacked one
        var targetHero : Hero =  heroes.getSurvialHero().find{ it.name == target } ?: return
        if(targetHero.beingTacticsed()){ return }
        //First attack hero
        var AttackHero : Hero = targetHero

        //The attack party has an attack -> false, until one of the parties have no attack -> true
        var EndDuel : Boolean = false
        while(!EndDuel){
            if(AttackHero.Cards.any{ it is AttackCard}){
                AttackHero.Cards.find { it is AttackCard }?.let { AttackHero.removeCard(it) }
                println("${AttackHero.name} used an Attack card --[Duel]")
                //Change the hero to Attack
                AttackHero = if(AttackHero == userHero) targetHero else userHero
                continue
            }
            println("${AttackHero.name} don't have any Attack card, ${AttackHero.name} reduced one hp. -[Duel]")
            //Default 1
            AttackHero.reduceHP(heroes.damageState)
            EndDuel = true
        }
    }
}

class Pilfer(): TacticsCard(){
    override var name = "pilfer"
    override fun ability(target : String) {}
    fun ability(target : String,user : String) {
        //With this target>continue, without >go back
        var userHero : Hero =  heroes.getSurvialHero().find{ it.name == user } ?: return
        var stealHero : Hero =  heroes.getSurvialHero().find{ it.name == target } ?: return
        if(stealHero.beingTacticsed()){ return }
        //Priority for equipment to remove
        //+1 Monnts
        if(stealHero.increasedMounts != null){
            println("${stealHero!!.name} was steel 1 equipment -- ${stealHero.increasedMounts!!.name}. -[Pilfer]")
            userHero.Cards.add(stealHero.increasedMounts!!)
            stealHero.increasedMounts = null
            return
        }
        //Armor
        if(stealHero.armor != null){
            println("${stealHero!!.name} was steel 1 equipment -- ${stealHero.armor!!.name}. -[Pilfer]")
            userHero.Cards.add(stealHero.armor!!)
            stealHero.armor = null
            return
        }
        //Weapon
        if(stealHero.weapon != null){
            println("${stealHero!!.name} was steel 1 equipment -- ${stealHero.weapon!!.name}. -[Pilfer]")
            userHero.Cards.add(stealHero.weapon!!)
            stealHero.weapon = null
            return
        }
        //-1 Monnts
        if(stealHero.reducedMounts != null){
            println("${stealHero!!.name} was steel 1 equipment -- ${stealHero.reducedMounts!!.name}. -[Pilfer]")
            userHero.Cards.add(stealHero.reducedMounts!!)
            stealHero.reducedMounts = null
            return
        }

        var randRemoveCardIndex : Int = Random.nextInt(0, stealHero.Cards.size)
        var removedcard : Card = stealHero.Cards.get(randRemoveCardIndex)

        stealHero.removeCard(removedcard)
        userHero.Cards.add(removedcard)

        println("${stealHero!!.name} was steel 1 card -- ${removedcard.name}. -[Pilfer]")
    }
}

class Duress(): TacticsCard(){
    override var name = "duress"
    override fun ability(target : String) {}
    fun ability(target : Hero,attackTarget : Hero,user: Hero) {
        println("${target.name} may play an “Attack” on another target hero.--[Duress]\n" +
                "If he does not, put that weapon card into ${user.name} hand --[Duress]")
        if(target.beingTacticsed()){ return }
        if(target.Cards.any{ it is AttackCard}){
            println("${target.name} spend 1 card to attack ${attackTarget.name} --[Duress]")
            target.Cards.find{ it is AttackCard}?.let { target.removeCard(it) }
            attackTarget.beingAttacked()
            if(!heroes.IsDodgeAttack){
                attackTarget.reduceHP(heroes.damageState)
            }
            return
        }
        user.Cards.add((target!!.weapon) as Card)
        println("${target!!.name} was steel the weapon -- ${target.weapon!!.name}. --[Duress]")
        target.weapon = null
    }
}

class ImpeccablePlan(): TacticsCard(){
    override var name = "ImpeccablePlan"
    override fun ability(target : String) {}
    fun ability(hero : Hero){
        println("${hero.name} use $name to cancel Tactics Card effort.")
    }
}

abstract class JudgmentCard(): Card,Command{
    override var name = ""
    override var suit: String = CardFactory.getRandomSuit()
    override var number : Int = CardFactory.getRandomNumber()
    abstract var nextJudgment : JudgmentCard?
    override fun ability(target : String) {}
    override fun execute(): Boolean { return Random.nextBoolean() }
}

class AcediaCard(): JudgmentCard(){
    override var nextJudgment : JudgmentCard? = null
    override var name = "acedia"
    override fun ability(target : String) {}
    fun ability(target : String,Card : AcediaCard) {
        //With this target>continue, without >go back
        var targetHero: Hero = heroes.getSurvialHero().find { it.name == target } ?: return
        ////If there are other judgment card
        if(targetHero.willjudgment) {
            while(targetHero.judgmentCard!!.nextJudgment != null){}
            targetHero.judgmentCard!!.nextJudgment = Card
            return
        }
        //Without judgment Card
        targetHero.setJudgment(Card)
    }
    override fun execute(): Boolean {
        val judgmentCard : Card = CardFactory.genCards()
        println("Acedia Judgment : ${judgmentCard.suit}")
        if(judgmentCard.suit == "heart") {
            println("Acedia fail. -[Acedia]")
            return false
        }
        println("Acedia success. Can't play Card. -[Acedia]")
        return true
    }
}

class LightningBoltCard(): JudgmentCard() {
    override var nextJudgment: JudgmentCard? = null
    override var name = "lightning bolt"
    override fun ability(target: String) {}
    fun ability(target: String, Card: LightningBoltCard) {
        //With this target>continue, without >go back
        var targetHero: Hero = heroes.getSurvialHero().find { it.name == target } ?: return
        //If there are other judgment cards
        if (targetHero.willjudgment) {
            while (targetHero.judgmentCard!!.nextJudgment != null) {
            }
            targetHero.judgmentCard!!.nextJudgment = Card
            return
        }
        //No judgment card
        targetHero.setJudgment(Card)
    }
}

fun setNextHero(hero : Hero,Card: LightningBoltCard){
    //Next hero
    var nextHero :Hero? = heroes.heroes[getNextHeroPosition(hero)]
    //If all are present
    if(!nextHero!!.alive) { setNextHero(nextHero,Card)}
    //If there are Judgment Card
    if(nextHero!!.willjudgment) {
        var nextHeroJCard : JudgmentCard? = nextHero!!.judgmentCard
        while(nextHeroJCard != null){
     //If there is already a Lightning Bolt, jump to the next one
            if(nextHeroJCard is LightningBoltCard) {
                setNextHero(nextHero,Card)
                return
            }
            nextHeroJCard = nextHeroJCard!!.nextJudgment
        }
        println("Lightning Bolt pass to ${nextHero.name}")
        nextHero.setJudgment(Card)
        return
    }
    println("Lightning Bolt pass to ${nextHero.name}")
    nextHero!!.setJudgment(Card)
}

fun getNextHeroPosition(hero : Hero):Int{
    //Monarch(0) -> Minister(1) -> Rebel(2) -> Traitor(3) -> Monarch(0)
    var positionNum : Int = when(hero.roleTitle){
        "Monarch"   ->  1
        "Minister"  ->  2
        "Rebel"     ->  3
        else        ->  0
    }
    //If the next hero hp<=0 -> then find the next one
    if(heroes.heroes[positionNum].hp <= 0) {
        return getNextHeroPosition(heroes.heroes[positionNum])
    }
    return positionNum
}

override fun execute(): Boolean {
    val judgmentCard : Card = CardFactory.genCards()
    println("Lightning Bolt Judgment : ${judgmentCard.suit} ${judgmentCard.number}")
    if(judgmentCard.suit != "spade" || judgmentCard.number < 2 || judgmentCard.number > 9) {
        println("Lightning Bolt fail. -[Lightning Bolt]")
        return false
    }
    heroes.damageCard = LightningBoltCard()
    println("Lightning Bolt success. get 3 Damages. -[Lightning Bolt]")
    return true
}


abstract class EquipmentCard() : Card{
    abstract override var name: String
    override var suit: String = CardFactory.getRandomSuit()
    override var number : Int = CardFactory.getRandomNumber()
    override fun ability(target : String) {}
}
abstract class WeaponCard() : EquipmentCard(){
    override var name = "Weapon Card"
    abstract var atkrange:Int
    override fun ability(target : String) {}
}
abstract class ArmorCard() : EquipmentCard(){
    override var name = "Armor Card"
    override fun ability(target : String) {}
    abstract fun ability() : Boolean
}
abstract class ReducedMountCard() : EquipmentCard(){
    override var name = "ReducedMount Card"
    override fun ability(target : String) {}
}
abstract class IncreasedMountCard() : EquipmentCard(){
    override var name = "IncreasedMount Card"
    override fun ability(target : String) {}
}







object CardFactory{

    fun getRandomNumber() : Int {
        return Random.nextInt(1, 14)
    }


    fun getRandomSuit() : String {
        return when(Random.nextInt(0, 4)){
            0-> "spade"
            1-> "heart"
            2-> "diamond"
            else -> "club"
        }
    }

    fun getColor(suit : String) : String {
        return when(suit){
            "heart" -> "red"
            "diamond" -> "red"
            else -> "black"
        }
    }


    fun genCards() : Card{
        val randNum : Int = Random.nextInt(0, 110)
        val card : Card = when{
            //0-34
            randNum < 35->     AttackCard()
            //35 - 49
            randNum < 49->     DodgeCard()
            //50 - 51
            randNum < 52->    EightTrigrams()
            //52
            randNum == 52->    SerpentSpear()
            //53 - 54
            randNum < 55->    ZCrossbow()
            //55
            randNum == 55->    SwordofBlueSteel()
            //56
            randNum == 56->    FrostBlade()
            //57 - 59
            randNum < 60->     BarbariansCard()
            //60
            randNum == 60->     HailofarrowsCard()
            //61-62
            randNum < 63->     OathofpeachCard()
            //63-64
            randNum < 65->     Harvest()
            //65-67
            randNum < 68->     Sleightofhand()
            //68-71
            randNum < 72->     BurnBridges()
            //72-74
            randNum < 75->     Pilfer()
            //75-79
            randNum < 80->     Duel()
            //80-81
            randNum < 82->    AcediaCard()
            //82-83
            randNum < 84->    LightningBoltCard()
            //84
            randNum == 84->    TwinSwords()
            //85
            randNum == 85->      AzureDragonCrescentBlade()
            //86
            randNum == 86->      RockCleavingAxe()
            //87
            randNum == 87->      RedHare()
            //88
            randNum == 88->      DaYuan()
            //89
            randNum == 89->      HuaLiu()
            //90
            randNum == 90->      TheShadow()
            //91
            randNum == 91->     KirinBow()
            //92
            randNum == 92->     HeavenHalberd()
            //93-94
            randNum < 95->      Duress()
            //95-98
            randNum < 99->      ImpeccablePlan()
            else->  PeachCard()
        }
        return card
    }
}



fun main() {
    for(x in heroes.heroes){
        x.initaldrawCards()
    }
    while(true){
        for (x in heroes.heroes) {
            heroes.heroturn = x
            if(x.hp > 0){
                x.templateMethod()
                println("\n")
            }
        }
    }
}