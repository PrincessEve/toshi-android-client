package com.tokenbrowser.view.activity;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.tokenbrowser.presenter.Presenter;
import com.tokenbrowser.presenter.PresenterLoader;
import com.tokenbrowser.presenter.factory.PresenterFactory;
import com.tokenbrowser.view.BaseApplication;

public abstract class BasePresenterActivity<P extends Presenter<V>, V> extends AppCompatActivity {
    private Presenter<V> presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LoaderCallbacks as an object, so no hint regarding Loader will be leak to the subclasses.
        getSupportLoaderManager().initLoader(loaderId(), null, new LoaderManager.LoaderCallbacks<P>() {
            @Override
            public final Loader<P> onCreateLoader(int id, Bundle args) {
                return new PresenterLoader<>(BasePresenterActivity.this, getPresenterFactory());
            }

            @Override
            public final void onLoadFinished(Loader<P> loader, P presenter) {
                BasePresenterActivity.this.presenter = presenter;
                onPresenterPrepared(presenter);
            }

            @Override
            public final void onLoaderReset(Loader<P> loader) {
                BasePresenterActivity.this.presenter = null;
                onPresenterDestroyed();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseApplication)getApplication()).applicationResumed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onViewAttached(getPresenterView());
    }

    @Override
    protected void onStop() {
        presenter.onViewDetached();
        super.onStop();
    }

    /**
     * Instance of {@link PresenterFactory} use to create a Presenter when needed. This instance should
     * not contain {@link android.app.Activity} context reference since it will be keep on rotations.
     */
    @NonNull
    protected abstract PresenterFactory<P> getPresenterFactory();

    /**
     * Hook for subclasses that deliver the {@link Presenter} before its View is attached.
     * Can be use to initialize the Presenter or simple hold a reference to it.
     */
    protected abstract void onPresenterPrepared(@NonNull P presenter);

    /**
     * Hook for subclasses before the screen gets destroyed.
     */
    protected void onPresenterDestroyed() {
    }

    /**
     * Override in case of fragment not implementing Presenter<View> interface
     */
    @NonNull
    protected V getPresenterView() {
        return (V) this;
    }

    /**
     * Use this method to specify a specific ID for the {@link PresenterLoader}.
     */
    protected abstract int loaderId();
}