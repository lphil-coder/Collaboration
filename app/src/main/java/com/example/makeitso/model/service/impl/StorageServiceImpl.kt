/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.example.makeitso.model.service.impl

import com.example.makeitso.COURSE_ID
import com.example.makeitso.COURSE_ID_ARG
import com.example.makeitso.model.Course
import com.example.makeitso.model.Task
import com.example.makeitso.model.service.StorageService
import com.google.firebase.firestore.DocumentChange.Type.REMOVED
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import javax.inject.Inject

class StorageServiceImpl @Inject constructor() : StorageService {

    private var listenerRegistration: ListenerRegistration? = null

    private var courseListenerRegistration: ListenerRegistration? = null

    companion object {
        private const val TASK_COLLECTION = "Task"
        private const val USER_ID = "userId"
        private const val COURSE_COLLECTION = "Course"
    }

    override fun addListener(
        userId: String,
        courseId: String,
        onDocumentEvent: (Boolean, Task) -> Unit,
        onError: (Throwable) -> Unit
    ) {
       //val query = Firebase.firestore.collection(COURSE_COLLECTION).document(course.id).collection(TASK_COLLECTION).whereEqualTo(COURSE_ID, course.id)
       // val query = Firebase.firestore.collection(COURSE_COLLECTION).whereEqualTo(COURSE_ID, courseId)
        val query = Firebase.firestore
            .collection(COURSE_COLLECTION)
            .document(courseId)
            .collection(TASK_COLLECTION)
            .whereEqualTo(USER_ID, userId)

            //COURSE_ID, courseId
       // val query = Firebase.firestore.collectionGroup(courseId)

        listenerRegistration = query.addSnapshotListener {
                value, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            value?.documentChanges?.forEach {
                val wasDocumentDeleted = it.type == REMOVED
                val task = it.document.toObject<Task>().copy(id = it.document.id)
                onDocumentEvent(wasDocumentDeleted, task)
            }
        }
    }

    override fun removeListener() {
        listenerRegistration?.remove()
    }

    override fun getTask(

        taskId: String,//id of task itself already existing
        courseId: String, //id of course, already existing
        onError: (Throwable) -> Unit,
        onSuccess: (Task) -> Unit

    ) {
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .document(courseId)
            .collection(TASK_COLLECTION)
            .document(taskId)
            .get()
            .addOnFailureListener { error -> onError(error) }
            .addOnSuccessListener { result ->
                val task = result.toObject<Task>()?.copy(id = result.id)
                onSuccess(task ?: Task())
            }
    }

    //Once you call the collection, it's implicitly created in the background. On the other hand,
    // to add a new item(document), use the .add, . add adds a new document to the collection with a n autogenerated Id
    //to update, use .set
//    override fun saveTask(
//        course: Course, //course already existing //instead we could get the entire course object and index course.id in our code instead
//        task: Task, //task not yet existing
//        onResult: (Throwable?) -> Unit) {
//        Firebase.firestore
//            .collection(COURSE_COLLECTION) //perhaps with a NOSQL, I may not need to reference the collection,
//            .document(course.id) //it could be that the taskID itself is enough(not having to follow some structured path)
//            .collection(TASK_COLLECTION)
//            .document()
//            .add(task)
//            .addOnCompleteListener { onResult(it.exception) }
//    }
    override fun saveTask(
        courseId: String, //course already existing //instead we could get the entire course object and index course.id in our code instead
        task: Task, //task not yet existing
        onResult: (Throwable?) -> Unit) {
        Firebase.firestore
            .collection(COURSE_COLLECTION) //perhaps with a NOSQL, I may not need to reference the collection,
            .document(courseId) //it could be that the taskID itself is enough(not having to follow some structured path)
            .collection(TASK_COLLECTION)
            .add(task)
            .addOnCompleteListener { onResult(it.exception) }
    }
    //fun updateTask(courseId: String, task: Task, onResult: (Throwable?) -> Unit)
    override fun updateTask(courseId: String, task: Task, onResult: (Throwable?) -> Unit) {
        Firebase.firestore
            .collection(COURSE_COLLECTION) //perhaps with a NOSQL, I may not need to reference the collection,
            .document(courseId) //it could be that the taskID itself is enough(not having to follow some structured path)
            .collection(TASK_COLLECTION)
            .document(task.id) //why task.id - are wee giving it a task object? SO why a taskid when getting
            .set(task)
            .addOnCompleteListener { onResult(it.exception) }
    }

    override fun deleteTask(courseId: String, taskId: String, onResult: (Throwable?) -> Unit) {
        Firebase.firestore
            .collection(COURSE_COLLECTION) //perhaps with a NOSQL, I may not need to reference the collection,
            .document(courseId) //it could be that the taskID itself is enough(not having to follow some structured path)
            .collection(TASK_COLLECTION)
            .document(taskId)//why taskId instead?
            .delete()
            .addOnCompleteListener {
                onResult(it.exception)
            }
    }

    override fun deleteAllForUser(userId: String, onResult: (Throwable?) -> Unit) {
//        Firebase.firestore
//            .collection(TASK_COLLECTION)
//            .whereEqualTo(USER_ID, userId)
//            .get()
//            .addOnFailureListener { error -> onResult(error) }
//            .addOnSuccessListener { result ->
//                for (document in result) document.reference.delete()
//                onResult(null)
//            }
        //According to Firestore documentation, when a collection is deleted, the subcollections are not automatically deleted
        //hence deleting the taskcollection first then the course collection
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .whereEqualTo(USER_ID, userId)
            .get()
            .addOnFailureListener { error -> onResult(error) }
            .addOnSuccessListener { result ->
                for (document in result) document.reference.delete()
                onResult(null)
            }
    }

    override fun updateUserId(
        oldUserId: String,
        newUserId: String,
        onResult: (Throwable?) -> Unit
    ) {
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .whereEqualTo(USER_ID, oldUserId)
            .get()
            .addOnFailureListener { error -> onResult(error) }
            .addOnSuccessListener { result ->
                for (document in result) document.reference.update(USER_ID, newUserId)
                onResult(null)
            }
    }





    override fun addCourseListener(
        userId: String,
        onDocumentEvent: (Boolean, Course) -> Unit,
        onError: (Throwable) -> Unit
    ) { val query2 = Firebase.firestore
            .collection(COURSE_COLLECTION)
            .whereEqualTo(USER_ID, userId)

        courseListenerRegistration = query2.addSnapshotListener { value, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            value?.documentChanges?.forEach {
                val wasDocumentDeleted = it.type == REMOVED
                val course = it.document.toObject<Course>().copy(id = it.document.id)
                onDocumentEvent(wasDocumentDeleted, course)
            }
        }
    }

    override fun removeCourseListener() {
        courseListenerRegistration?.remove() //changed listenerRegistration to courseListener registration for anything related to courses
    }

    override fun getCourse(
        courseId: String, //this is used to reference already existing courses
        onError: (Throwable) -> Unit,
        onSuccess: (Course) -> Unit
    ) {
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .document(courseId)
            .get()
            .addOnFailureListener { error -> onError(error) }
            .addOnSuccessListener { result ->
                val course = result.toObject<Course>()?.copy(id = result.id)
                onSuccess(course ?: Course())
            }
    }

    //Once you call the collection, it's implicitly created in the background. On the other hand,
    // to add a new item(document), use the .add, . add adds a new document to the collection with a n autogenerated Id
    //to update, use .set
    override fun saveCourse(course: Course, onResult: (Throwable?) -> Unit) {
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .add(course)//an id is probably automatically generated here
            .addOnCompleteListener { onResult(it.exception) }
    }

    override fun updateCourse(course: Course, onResult: (Throwable?) -> Unit) {
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .document(course.id) //an id is referenced
            .set(course)
            .addOnCompleteListener { onResult(it.exception) }
    }

    override fun deleteCourse(courseId: String, onResult: (Throwable?) -> Unit) {
        Firebase.firestore
            .collection(COURSE_COLLECTION)
            .document(courseId) //all we do here is reference id created
            .delete()
            .addOnCompleteListener { onResult(it.exception) }
    }

}