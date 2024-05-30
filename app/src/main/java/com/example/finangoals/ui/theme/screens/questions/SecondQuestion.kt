package com.example.finangoals.ui.theme.screens.questions


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondQuestion(navController: NavHostController) {
    /*
     Creamos una variable 'textIn' que se encargará de guardar la información introducida por el usuario
     con la función "rememberSaveable(stateSaver = TextFieldValue.Saver)" conseguimos que guarde
     y restaure automáticamente el estado de la UI además de decirle cómo tiene que guardar los datos
     recogidos del usuario y que éstos no se pierdan
      */
    var textIn by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // Recuperamos el parámetro ingresos de la pantalla anterior
    val ingresos = navController.previousBackStackEntry?.savedStateHandle?.get<Float>("ingresos") ?: 0f
    /*
    Configuramos un Scaffold, éste actúa como un contenedor, gestionando y manteniendo unidos los elementos
    de la UI siguiendo la guía de Material Design. En este caso el Scaffold englobará un
    topBar. Ya dentro del Scaffold, definimos por un lado la barra superior asignándole un título, y los estilos definidos
    en el Theme de nuestra App, y por otro lado en el 'content', definiremos el resto del contenido
   de la aplicación que está fuera del AppBar.
     */

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FinanGoals")},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Orange,
                    titleContentColor = Color.White
                )
            )
        },
        /*
       Aquí pondremos todos los componentes que conforman la app y que están debajo del AppBar,
       'content' se le pasa una función lambda que recibe un 'innerPadding' para ajustar el contenido principal al resto de
       elementos del Scaffold, evitando el solapamiento de los componentes.
        */
        content = { paddingValues ->
            /*
            Configuramos un componente Column, como componente principal para organizar verticalmente los elementos tales como,
            un Text y un OutlinedTextField (componente que permite la lectura y escritura de datos.
            Y un Row dentro del Column para organizar los elementos en horizontal,  aquí tenemos elementos
            para la navegación como TextButton (combinación de texto e icono mejorando la accesibilidad).
             */

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(White)
                    .padding(paddingValues)
                    .padding(horizontal = 48.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Indica tus gastos mensuales aproximados; esto incluye: alquileres, préstamos, hipotecas, tarjetas, gastos corrientes (internet, luz, gas...) y gastos no corrientes (seguros, ocio, imprevistos...).",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = textIn,
                    onValueChange = { textIn = it },
                    label = { Text("Gastos") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMsg != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        navController.navigate(NavRoutes.FirstQuestion.route)
                    },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) {
                        Text("Cancelar")
                    }
                    /*
                   Al hacer click el usuario, la función lambda llamará a la función de validación para comprobar que los
                   datos (textIn) son correctos, la función nos devuelve un objeto de tipo valResult que será con el que comprobaremos
                   si los datos introducidos son válidos o no.
                    */
                    Button(onClick = {
                        val valiResult = validateNumInput(textIn)
                        if (valiResult.isValid) {
                            val gastos = textIn.toFloat() // Convertimos el campo texto a número
                            // Navegamos pasando los argumentos
                            navController.navigate(NavRoutes.Summary.createRoute(ingresos, gastos))
                        } else {
                            errorMsg = valiResult.error
                        }
                    },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) {
                        Text("Continuar")
                    }
                }
            }
        }
    )
}


//Definimos el data class ValiResult que podrá tener dos valores isValid o error
data class ValiResult(val isValid: Boolean, val error: String?)

/*Función de validación del campo ingresos, recibe un String y devuelve un objeto ValiResult
Si el campo texto se puede convertir a número entonces el objeto ValiResult es válido de
lo contrario dará un mensaje de error al usuario
 */
private fun validateNumInput(input: String): ValiResult {
    return if (input.toDoubleOrNull() != null) ValiResult(true, null)
    else ValiResult(false, "Datos incorrectos, el campo tiene que ser numérico")
}

@Preview(showBackground = true)
@Composable
fun SecondQuestionShow() {
    val navController = rememberNavController()
    SecondQuestion(navController)
}