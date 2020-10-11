object PlainCaseClassModels {
  case class User(id: UserId, email: Email, age: Age)

  case class UserId(value: String) extends AnyVal
  object UserId {
    // Smart constructor
    def apply(rawUserId: String): Option[UserId] =
      Some(rawUserId).filter(isValidUserId).map(new UserId(_))

    // Rule
    private def isValidUserId(s: String): Boolean =
      s.startsWith("@") && s.length >= 3 && s.length <= 16 && !s.matches("(?i)@admin")
  }

  case class Email(value: String) extends AnyVal
  object Email {
   // Smart constructor
    def apply(rawEmail: String): Option[Email] =
      Some(rawEmail).filter(isValidEmail).map(new Email(_))

    // Rule
    private def isValidEmail(s: String): Boolean =
      s.matches("""[a-z0-9]+@[a-z0-9]+\.[a-z0-9]{2,}""")
  }

  case class Age(value: Int) extends AnyVal
  object Age {
    // Smart constructor
    def apply(rawAge: Int): Option[Age] =
      Some(rawAge).filter(isValidAge).map(new Age(_))

    // Rule
    private def isValidAge(i: Int): Boolean = i >= 0 && i <= 200
  }
}

object SmartConstructorExample extends App {

  import cats.data.ValidatedNel
  import cats.implicits._
  import PlainCaseClassModels._

  def validateUser(rawUserId: String,
    rawEmail: String,
    rawAge: Int): ValidatedNel[String, User] =
    (
      UserId(rawUserId).toValidNel(s"Invalid UserId: $rawUserId"),
      Email(rawEmail).toValidNel(s"Invalid Email: $rawEmail"),
      Age(rawAge).toValidNel(s"Invalid Age: $rawAge")
      ).mapN(User)

  println(validateUser("@todokr", "tadokoro@example.com", 29))
  // Valid(User(UserId(@todokr),Email(tadokoro@example.com),Age(29)))

  println(validateUser("@ADMIN", "tadokoro.example.com", -1))
  // Invalid(NonEmptyList(Invalid UserId: @ADMIN, Invalid Email: tadokoro.example.com, Invalid Age: -1))
}

object RefinedExample extends App {
  import eu.timepit.refined.api.Refined
  import eu.timepit.refined.boolean.{And, Not}
  import eu.timepit.refined.collection.{MaxSize, MinSize, NonEmpty}
  import eu.timepit.refined.string.{MatchesRegex, StartsWith}
  import eu.timepit.refined.auto._

  // `@`から始まって3文字以上16文字以内、@adminや@Adminは許容しない
  type UserIdRule = StartsWith["@"] And MinSize[3] And MaxSize[16] And Not[MatchesRegex["(?i)@admin"]]

  val userId1: String Refined UserIdRule = "@todokr"

  // 以下はコンパイルエラー
  // val userId2: String Refined UserIdRule = "@admin"
  // val userId3: String Refined UserIdRule = "todokr"
  // val userId4: String Refined UserIdRule = "@uryyyyyyyyyyyyyy"

  type Id = String Refined NonEmpty
  type Password = String Refined NonEmpty

  case class LoginInfo(id: Id, password: Password)

  val id: Id = "myid123"
  val password: Password = "Passw0rd!"

  val loginInfo = LoginInfo(id = password, password = id) // コンパイルできてしまう😢
  println(s"id: ${loginInfo.id}")
  println(s"password: ${loginInfo.password}")
}

object RefineWithNewtypeModels {
  import eu.timepit.refined.api.Refined
  import eu.timepit.refined.boolean.{And, Not}
  import eu.timepit.refined.collection.{MaxSize, MinSize}
  import eu.timepit.refined.numeric.NonNegative
  import eu.timepit.refined.refineV
  import eu.timepit.refined.string.{MatchesRegex, StartsWith}
  import io.estatico.newtype.macros.newtype

  case class User(userId: UserId, email: Email, age: Age)

  type UserIdRule =
    StartsWith["@"] And
      MinSize[3] And
      MaxSize[16] And
      Not[MatchesRegex["(?i)@admin"]]

  type EmailRule = MatchesRegex["""[a-z0-9]+@[a-z0-9]+\.[a-z0-9]{2,}"""]
  type AgeRule = NonNegative

  type UserIdString = String Refined UserIdRule
  type EmailString = String Refined EmailRule
  type AgeInt = Int Refined AgeRule

  @newtype case class UserId(value: UserIdString)
  object UserId {
    def apply(rawUserId: String): Either[String, UserId] = refineV[UserIdRule](rawUserId).map(UserId(_))
  }
  @newtype case class Email(value: EmailString)
  object Email {
    def apply(rawEmail: String): Either[String, Email] = refineV[EmailRule](rawEmail).map(Email(_))
  }
  @newtype case class Age(value: AgeInt)
  object Age {
    def apply(rawAge: Int): Either[String, Age] = refineV[AgeRule](rawAge).map(Age(_))
  }
}

object RefineWithNewtypeExample extends App {

  import cats.data.ValidatedNel
  import cats.implicits._
  import RefineWithNewtypeModels._

  def validateUser(
    rawUserId: String,
    rawEmail: String,
    rawAge: Int): ValidatedNel[String, User] =
      (
        UserId(rawUserId).toValidatedNel,
        Email(rawEmail).toValidatedNel,
        Age(rawAge).toValidatedNel
      ).mapN(User)

  println(validateUser("@todokr", "tadokoro@example.com", 29))
  // Valid(User(UserId(@todokr),Email(tadokoro@example.com),Age(29)))

  println(validateUser("@ADMIN", "tadokoro.example.com", -1))
  // Invalid(NonEmptyList(Invalid UserId: @ADMIN, Invalid Email: tadokoro.example.com, Invalid Age: -1))
}

object CoercibleCirceCodecs {
  import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
  import io.circe.refined._ // unusedに見えるが実は使われている
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.estatico.newtype.Coercible
  import io.estatico.newtype.ops._
  import RefineWithNewtypeModels.User

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]
    : KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]
    : KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])

  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
}

object WorkWithJsonExample extends App {
  import eu.timepit.refined.auto.autoUnwrap // F[T, P]をTにunwrapしてくれる
  import io.circe.parser.decode
  import CoercibleCirceCodecs._
  import RefineWithNewtypeModels._

  Iterator
    .continually(Console.in.readLine())
    .takeWhile(_ != ":quit")
    .foreach { rawMessage =>
      decode[User](rawMessage) match {
        case Left(value) => println(value)
        case Right(msg) =>
          println(s"userId: ${msg.userId}")
          println(s"age: ${msg.age}")
          println(s"email: ${msg.email}")
      }

      println("*" * 30)
      println()
    }
}

object CoercibleDoobieCodec {
  import cats.Eq
  import doobie.{Put, Read}
  import io.estatico.newtype.Coercible

  implicit def newTypePut[R, N](implicit ev: Coercible[Put[R], Put[N]], R: Put[R]): Put[N] = ev(R)
  implicit def newTypeRead[R, N](implicit ev: Coercible[Read[R], Read[N]], R: Read[R]): Read[N] = ev(R)

  /** derive an Eq instance for newtype N from Eq instance for Repr type R */
  implicit def coercibleEq[R, N](implicit ev: Coercible[Eq[R], Eq[N]], R: Eq[R]): Eq[N] = ev(R)
}

object WorkWithDbExample extends App {
  import doobie.{ExecutionContexts, Transactor}
  import doobie.implicits._
  import doobie.refined.implicits._
  import cats.effect.{Blocker, ContextShift, IO}
  import RefineWithNewtypeModels._

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)
  val tx: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:9999/test",
    user = "root",
    pass = "root",
    blocker = Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

  final class UserRepository(transactor: Transactor[IO]) {
    import CoercibleDoobieCodec._

    def find(email: String): IO[Option[User]] = {
      sql"""SELECT user_id,  email, age
           |FROM users
           |WHERE email=$email
           |""".stripMargin
        .query[User]
        .option
        .transact(transactor)
    }
  }

  val repository = new UserRepository(tx)
  val maybeUser = repository.find("tadokoro@example.com").unsafeRunSync()
  println(maybeUser)
}

