package com.example.finangoals.ui.theme.screens.login



import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.finangoals.R
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.OrangeSec
import com.example.finangoals.ui.theme.theme.White
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(navController: NavHostController, authController: AuthController) {
    /*
    Creamos las variables que manejan el inicio de sesión tanto con usuario y contraseña, como con Google
    */

    /*
    Creamos las variables 'textUser' y 'textPass' que se encargarán de guardar la información introducida por el usuario
    con la función "rememberSaveable(stateSaver = TextFieldValue.Saver)" conseguimos que guarde
    y restaure automáticamente el estado de la UI además de decirle cómo tiene que guardar los datos
    recogidos del usuario y que éstos no se pierdan
     */
    var textUser by remember { mutableStateOf("") }
    var textPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var signInRequested by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(signInRequested) {
        if (signInRequested) {
            // Validar las entradas
            emailError = if (textUser.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(textUser).matches()) {
                "Por favor, ingresa un correo electrónico válido."
            } else {
                null
            }

            passwordError = if (textPass.length < 6) {
                "La contraseña debe tener al menos 6 caracteres."
            } else {
                null
            }

            // Si no hay errores, intentar iniciar sesión
            if (emailError == null && passwordError == null) {
                val result = authController.signInWithEmailPassword(textUser, textPass)
                when (result) {
                    is AuthRes.Exito -> {
                        navController.navigate(NavRoutes.Home.route)
                        Toast.makeText(context, "Sesión iniciada", Toast.LENGTH_SHORT).show()
                        signInRequested = false
                    }
                    is AuthRes.Error -> {
                        Toast.makeText(context, "El usuario no existe o la contraseña es incorrecta", Toast.LENGTH_LONG).show()
                        signInRequested = false
                    }
                }
            } else {
                signInRequested = false
            }
        }
    }


    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()) { result ->
        when(val accountResult = authController.handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(result.data))) {
            is AuthRes.Exito -> {
                val credential = GoogleAuthProvider.getCredential(accountResult.data.idToken, null)
                scope.launch {
                    val authResponse = authController.signInWithGoogleCredential(credential)
                    when (authResponse) {
                        is AuthRes.Exito -> {
                            Toast.makeText(context, "Sesión iniciada", Toast.LENGTH_SHORT).show()
                            val fireUser = authResponse.data
                            val isNewUser = authResponse.isNewUser
                            if (isNewUser) {
                                navController.navigate(NavRoutes.FirstQuestion.route) {
                                    popUpTo(NavRoutes.Login.route) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                navController.navigate(NavRoutes.Home.route) {
                                    popUpTo(NavRoutes.Login.route) {
                                        inclusive = true
                                    }
                                }
                            }
                        }
                        is AuthRes.Error -> {
                            Toast.makeText(context, "No se ha podido iniciar sesión con la cuenta de Google: ${authResponse.errorMessage}", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "Error desconocido", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            is AuthRes.Error -> {
                Toast.makeText(context, "No se ha podido iniciar sesión", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Error desconocido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*
    Configuramos un Scaffold, éste actúa como un contenedor, gestionando y manteniendo unidos los elementos
    de la UI siguiendo la guía de Material Design. En este caso el Scaffold englobará un
    topBar. Ya dentro del Scaffold, definimos por un lado la barra superior asignándole un título, y los estilos definidos
    en el Theme de nuestra App, y por otro lado en el 'content', definiremos el resto del contenido
   de la aplicación que está fuera del AppBar.
     */

    Scaffold(
        content = { innerPadding ->
            /*
            Configuramos un componente Box que dentro tendrá:
                    Column, como componente principal para organizar verticalmente los elementos tales como,
                    un Text y un OutlinedTextField (componente que permite la lectura y escritura de datos.
                    Y un Row dentro del Column para organizar los elementos en horizontal,  aquí tenemos elementos
                    para la navegación como TextButton (combinación de texto e icono mejorando la accesibilidad).
             */
            Box(modifier = Modifier.padding(innerPadding)) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Spacer(modifier = Modifier.height(32.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_vec),
                        contentDescription = "Icono de la aplicación",
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    NombreApp()

                    Spacer(modifier = Modifier.height(24.dp))

                   // EmailInputField(textUser = textUser, onTextChange = { textUser = it })
                    OutlinedTextField(
                        value = textUser,
                        onValueChange = { textUser = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFA500), // Color naranja en formato ARGB
                            focusedLabelColor = Color(0xFFFFA500),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Gray
                        ),
                        isError = emailError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None
                        )
                    )
                    emailError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }


                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = textPass,
                        onValueChange = { textPass = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFA500),
                            focusedLabelColor = Color(0xFFFFA500),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Gray
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { // Icono interactivo para alternar la visibilidad
                            val image = if (passwordVisible) R.drawable.eye else R.drawable.eyelash
                            val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    painter = painterResource(id = image),
                                    contentDescription = description
                                )
                            }
                        },

                        isError = passwordError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            capitalization = KeyboardCapitalization.None
                        ),
                    )
                    passwordError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }


                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            signInRequested = true
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange
                        )
                    ) {
                        Text("Iniciar sesión",
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "O" ,
                    )

                    Spacer(modifier = Modifier.height(8.dp))


                    Button(
                        onClick = {
                            authController.signInWithGoogle(googleSignInLauncher)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = White
                        )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_google),
                            contentDescription = "",
                            modifier = Modifier.padding(top = 3.dp, bottom = 3.dp, end = 12.dp)
                        )
                        Text(text = "Continuar con Google",  fontSize = 16.sp)
                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Si aún no tienes cuenta, " ,
                        )
                        TextButton(
                            onClick = { navController.navigate(NavRoutes.Signup.route) },
                            contentPadding = PaddingValues()
                        ) {
                            Row(horizontalArrangement = Arrangement.Start) {
                                Text(text = "regístrate.",
                                    fontSize = 20.sp,
                                    color = OrangeSec
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    )
}

@Composable
fun NombreApp() {
    val texto = "FinanGoals"
    val fuenteTitulo = FontFamily(Font(R.font.shadowsintolightregular))
    val textoColoreado = buildAnnotatedString {
        // Primera mitad del texto
        withStyle(style = SpanStyle(color = Orange)) {
            append(texto.substring(0, 5))
        }

        // Segunda mitad del texto
        withStyle(style = SpanStyle(color = Color.Black)) {
            append(texto.substring(5, texto.length))
        }
    }
    Text(
        text = textoColoreado,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally),
        style = TextStyle(
            fontFamily = fuenteTitulo,
            fontSize = 70.sp
        )
    )
}



//Hacemos una preview para ver en tiempo real cómo va quedando nuestra aplicación
@Preview(showBackground = true)
@Composable
fun LoginShow() {
    // Esto es solo para fines de visualización y no reflejará la lógica de navegación real
    val navController = rememberNavController()
    LoginScreen(navController, AuthController(LocalContext.current))
}
