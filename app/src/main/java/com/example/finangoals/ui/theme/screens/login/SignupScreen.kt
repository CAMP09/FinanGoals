

import android.widget.Toast
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.finangoals.R
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.screens.login.AuthController
import com.example.finangoals.ui.theme.screens.login.AuthRes
import com.example.finangoals.ui.theme.theme.Orange



@Composable
fun SignupScreen(navController: NavHostController, authController: AuthController) {

    var textUser by remember { mutableStateOf("") }
    var textPass by remember { mutableStateOf("") }
    var signUpRequested by remember { mutableStateOf(false) }  // Controla cuándo se ha solicitado el registro
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(signUpRequested) {
        if (signUpRequested) {
            // Validación de las entradas
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

            // Si no hay errores, intentar registrar el usuario
            if (emailError == null && passwordError == null) {
                val result = authController.signUpWithEmailPassword(textUser, textPass)
                when (result) {
                    is AuthRes.Exito -> {
                        navController.navigate(NavRoutes.FirstQuestion.route)
                        signUpRequested = false
                    }
                    is AuthRes.Error -> {
                        Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                        signUpRequested = false
                    }
                }
            } else {
                signUpRequested = false
            }
        }
    }

    Scaffold(
        content = { innerPadding ->

            Box(modifier = Modifier.padding(innerPadding)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Spacer(modifier = Modifier.height(32.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_vec),
                        contentDescription = "Icono de la aplicación",
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    NombreApp()
                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField( //componente específico para la entrada de texto
                        value = textUser,
                        onValueChange = { textUser = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Orange,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Gray,
                            focusedLabelColor = Orange
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

                    OutlinedTextField( //componente específico para la entrada de texto
                        value = textPass,
                        onValueChange = { textPass = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Orange,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Gray,
                            focusedLabelColor = Orange
                        ),
                        visualTransformation =  PasswordVisualTransformation(),
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
                            signUpRequested = true // Establecer a true para iniciar el proceso de registro
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange
                        )
                    ) {
                        Text("Registrarse",
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
            fontSize = 80.sp
        )
    )
}



//Hacemos una preview para ver en tiempo real cómo va quedando nuestra aplicación
@Preview(showBackground = true)
@Composable
fun SignupShow() {
    // Esto es solo para fines de visualización y no reflejará la lógica de navegación real
    val navController = rememberNavController()
    SignupScreen(navController, AuthController(LocalContext.current))
}
