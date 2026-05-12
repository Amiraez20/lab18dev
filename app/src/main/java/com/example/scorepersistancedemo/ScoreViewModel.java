package com.example.scorepersistancedemo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import androidx.lifecycle.SavedStateHandle;

public class ScoreViewModel extends ViewModel {

    private static final String CLE_PERSISTANCE = "score_persistant";
    private final SavedStateHandle etatSauvegarde;
    private final MutableLiveData<Integer> valeurScore = new MutableLiveData<>();

    public ScoreViewModel(SavedStateHandle handle) {
        this.etatSauvegarde = handle;
        Integer valeurSauvee = etatSauvegarde.get(CLE_PERSISTANCE);
        valeurScore.setValue(valeurSauvee != null ? valeurSauvee : 0);
    }

    public void augmenter() {
        Integer val = valeurScore.getValue();
        if (val != null) {
            int nouvelleVal = val + 1;
            valeurScore.setValue(nouvelleVal);
            etatSauvegarde.set(CLE_PERSISTANCE, nouvelleVal);
        }
    }

    public void diminuer() {
        Integer val = valeurScore.getValue();
        if (val != null) {
            int nouvelleVal = val - 1;
            valeurScore.setValue(nouvelleVal);
            etatSauvegarde.set(CLE_PERSISTANCE, nouvelleVal);
        }
    }

    public void reinitialiser() {
        valeurScore.setValue(0);
        etatSauvegarde.set(CLE_PERSISTANCE, 0);
    }

    public LiveData<Integer> obtenirScore() {
        return valeurScore;
    }

    public void augmenterDepuisThread() {
        new Thread(() -> {
            Integer val = valeurScore.getValue();
            if (val != null) {
                valeurScore.postValue(val + 1);
            }
        }).start();
    }
}