package com.example.finangoals.db

import android.content.Context
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

data class FirestoreManagerData(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val endDate: String = "",
    val userId: String = "",
    var aport: Double= 0.0
)

data class TransactionData(
    var description: String = "",
    var amount: Double = 0.0,
    var month: Int = LocalDate.now().monthValue - 1,
    var type: TransactionType = TransactionType.EXPENSE,
    var id: String = "",
    val userId: String = ""
)
enum class TransactionType { INCOME, EXPENSE }

class FirestoreManager(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    // Añadir una meta financiera
    fun addGoal(goal: FirestoreManagerData, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val goalWithUserId = goal.copy(userId = currentUser.uid)
            db.collection("goals")
                .add(goalWithUserId)
                .addOnSuccessListener { documentReference ->
                    val goalWithId = goalWithUserId.copy(id = documentReference.id)
                    db.collection("goals").document(documentReference.id)
                        .set(goalWithId)
                        .addOnSuccessListener {
                            onSuccess(documentReference.id)
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Error al actualizar el ID del documento")
                        }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al añadir el documento")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Modificar una meta financiera
    fun update(goalId: String, goal: FirestoreManagerData, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val updatedGoal = goal.copy(id = goalId, userId = currentUser.uid)
            db.collection("goals").document(goalId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("userId") == currentUser.uid) {
                        db.collection("goals").document(goalId).set(updatedGoal)
                            .addOnSuccessListener {
                                onSuccess(goalId)
                            }
                            .addOnFailureListener { e ->
                                onFailure(e.message ?: "Error al modificar el documento")
                            }
                    } else {
                        onFailure("Permiso denegado")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al verificar el permiso")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Eliminar una meta financiera
    fun delete(goalId: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("goals").document(goalId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("userId") == currentUser.uid) {
                        db.collection("goals").document(goalId).delete()
                            .addOnSuccessListener {
                                onSuccess(goalId)
                            }
                            .addOnFailureListener { e ->
                                onFailure(e.message ?: "Error al eliminar el documento")
                            }
                    } else {
                        onFailure("Permiso denegado")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al verificar el permiso")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Buscar en la base de datos una meta financiera
    fun getGoal(goalId: String, onSuccess: (FirestoreManagerData) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val docRef = db.collection("goals").document(goalId)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("userId") == currentUser.uid) {
                        val goal = document.toObject(FirestoreManagerData::class.java)
                        if (goal != null) {
                            onSuccess(goal)
                        } else {
                            onFailure("Error al obtener el documento")
                        }
                    } else {
                        onFailure("Permiso denegado")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Listado de todas las metas de un usuario
    fun listAllGoals(onSuccess: (List<FirestoreManagerData>) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("goals")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val goalsList = mutableListOf<FirestoreManagerData>()
                    for (document in result) {
                        document.toObject(FirestoreManagerData::class.java).let {
                            goalsList.add(it)
                        }
                    }
                    onSuccess(goalsList)
                }
                .addOnFailureListener { exception ->
                    onFailure(exception.message ?: "Error desconocido al listar metas")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Métodos para manejar transacciones
    fun addTransaction(transaction: TransactionData, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val transactionWithUserId = transaction.copy(userId = currentUser.uid)
            db.collection("transactions")
                .add(transactionWithUserId)
                .addOnSuccessListener { documentReference ->
                    val transactionWithId = transactionWithUserId.copy(id = documentReference.id)
                    db.collection("transactions").document(documentReference.id)
                        .set(transactionWithId)
                        .addOnSuccessListener {
                            calculateSavings(
                                onSuccess = { onSuccess(documentReference.id) },
                                onFailure = { error -> onFailure("Error al calcular ahorros: $error") }
                            )
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Error al actualizar el ID del documento")
                        }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al añadir el documento")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Actualizar una transacción
    fun updateTransaction(transactionId: String, newTransactionData: TransactionData, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val updatedTransaction = newTransactionData.copy(id = transactionId, userId = currentUser.uid)
            db.collection("transactions").document(transactionId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("userId") == currentUser.uid) {
                        db.collection("transactions").document(transactionId).set(updatedTransaction)
                            .addOnSuccessListener {
                                calculateSavings(
                                    onSuccess = { onSuccess() },
                                    onFailure = { error -> onFailure("Error al calcular ahorros: $error") }
                                )
                            }
                            .addOnFailureListener { e ->
                                onFailure(e.message ?: "Error al modificar el documento")
                            }
                    } else {
                        onFailure("Permiso denegado")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al verificar el permiso")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Obtener una transacción
    fun getTransaction(transactionId: String, onSuccess: (TransactionData) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("transactions").document(transactionId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("userId") == currentUser.uid) {
                        val transaction = document.toObject(TransactionData::class.java)
                        if (transaction != null) {
                            onSuccess(transaction)
                        } else {
                            onFailure("Error al obtener el documento")
                        }
                    } else {
                        onFailure("Permiso denegado")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Eliminar una transacción
    fun deleteTransaction(transactionId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("transactions").document(transactionId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("userId") == currentUser.uid) {
                        db.collection("transactions").document(transactionId).delete()
                            .addOnSuccessListener {
                                calculateSavings(
                                    onSuccess = { onSuccess() },
                                    onFailure = { error -> onFailure("Error al calcular ahorros: $error") }
                                )
                            }
                            .addOnFailureListener { e ->
                                onFailure(e.message ?: "Error al eliminar el documento")
                            }
                    } else {
                        onFailure("Permiso denegado")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al verificar el permiso")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Listar todas las transacciones de un usuario
    fun listTransactions(onSuccess: (List<TransactionData>) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("transactions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("month", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    val transactions = result.documents.mapNotNull { it.toObject(TransactionData::class.java) }
                    onSuccess(transactions)
                }
                .addOnFailureListener { exception ->
                    onFailure(exception.message ?: "Error desconocido al listar transacciones")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    // Método para calcular el ahorro total
    fun calculateSavings(onSuccess: (Double) -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("transactions")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    val totalIncome = result.documents.filter {
                        (it["type"] == TransactionType.INCOME.toString())
                    }.sumOf { it.getDouble("amount") ?: 0.0 }

                    val totalExpense = result.documents.filter {
                        (it["type"] == TransactionType.EXPENSE.toString())
                    }.sumOf { it.getDouble("amount") ?: 0.0 }

                    val savings = totalIncome - totalExpense
                    onSuccess(savings)
                }
                .addOnFailureListener { exception ->
                    onFailure(exception.message ?: "Error al calcular ahorros")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    fun updateGoalAport(goalId: String, newAportacion: Double, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val goalRef = db.collection("goals").document(goalId)
            goalRef.update("aport", newAportacion)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e.message ?: "Error desconocido") }
        } else {
            onFailure("Usuario no autenticado")
        }
    }

    fun addInitialTransactions(ingresos: Double, gastos: Double, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Crear transacción de ingreso
            val incomeTransaction = TransactionData(
                description = "Ingresos habituales",
                amount = ingresos,
                month = LocalDate.now().monthValue - 1,
                type = TransactionType.INCOME,
                userId = currentUser.uid
            )

            // Crear transacción de gasto
            val expenseTransaction = TransactionData(
                description = "Gastos habituales",
                amount = gastos,
                month = LocalDate.now().monthValue - 1,
                type = TransactionType.EXPENSE,
                userId = currentUser.uid
            )

            // Añadir las transacciones iniciales
            val incomeRef = db.collection("transactions").document()
            incomeTransaction.id = incomeRef.id
            incomeRef.set(incomeTransaction)
                .addOnSuccessListener {
                    val expenseRef = db.collection("transactions").document()
                    expenseTransaction.id = expenseRef.id
                    expenseRef.set(expenseTransaction)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Error al añadir transacción de gasto inicial")
                        }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Error al añadir transacción de ingreso inicial")
                }
        } else {
            onFailure("Usuario no autenticado")
        }
    }
}

